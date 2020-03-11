package cs455.scaling.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import cs455.scaling.task.AcceptClientConnection;
import cs455.scaling.task.Constants;
import cs455.scaling.task.OrganizeBatch;
import cs455.scaling.task.Task;

/*
1.1 Server Node:
There is exactly one server node in the system. The server node provides the following functions:
A. Accepts incoming network connections from the clients.
B. Accepts incoming traffic from these connections
C. Groups data from the clients together into batches
D. Replies to clients by sending back a hash code for each message received.
E. The server performs functions A, B, C, and D by relying on the thread pool. 
*/
public class Server {
	private LinkedList<String> hashList;						//stores hashed byte[] received from clients
	private final LinkedBlockingQueue<Task> queue;				//stores entire tasks to handle
	private final ArrayList<ClientData> channelsToHandle;		//Stores the clients with data to handle
	private final Hashtable<String, Integer> currentClients;				//

	private Timer timer;										//handles output + batch timeouts
	private ScheduleTimeout timeout;
	private Semaphore organizeLock;

	private final int batchSize;								//max # of clients in each batch
	private final int batchTime;								//time before batch is automatically sent
	private final AtomicInteger messagesReceived;				//keep track of incoming messages
	private final AtomicInteger messagesSent;					//keep track of outgoing messages
	private final AtomicInteger connectedClients;				//number of concurrent clients


	private ServerSocketChannel serverChannel;							//the hub for connections from clients to come in on
	private Selector selector;									//selects from the available connections

	private ThreadPoolManager manager;
	
	public Server(int poolSize, int batchSize, int batchTime) {
		hashList = new LinkedList<String>();
		queue = new LinkedBlockingQueue<>();
		channelsToHandle = new ArrayList<>();
		currentClients = new Hashtable<>();

		messagesReceived = new AtomicInteger();
		messagesSent = new AtomicInteger();
		connectedClients = new AtomicInteger();

		organizeLock = new Semaphore(1);

		this.batchSize = batchSize;
		this.batchTime = batchTime;

		//doesn't start them
		manager = new ThreadPoolManager(poolSize, batchSize, queue);
	}

	public static void main(String[] args) {
		
		//args in form of portnum, thread-pool-size, batch-size, batch-time
		if (args.length != 4) {
			System.out.println("Error. Invalid # of parameters. (PORT POOLSIZE BATCHSIZE BATCHTIME");
			return;
		}

		//pull arguments
		int port = Integer.parseInt(args[0]);
		int poolSize = Integer.parseInt(args[1]);
		int batchSize = Integer.parseInt(args[2]);
		int batchTime = Integer.parseInt(args[3]);

		//initialize the server
		Server server = new Server(poolSize, batchSize, batchTime);

		//setup the serverSocket to listen to incoming connections, start the threadPool, and create the timer
		if (!server.configureAndStart(port))
			return;

		try {
			server.channelPolling();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private boolean configureAndStart(int port) {
		//setup the server NIO
		try {
			//get local host to bind
			String host = InetAddress.getLocalHost().getHostName();
//			host = "localhost";
			System.out.printf("Listening on: Host: %s, Port: %d%n", host, port);

			selector = Selector.open();

			serverChannel = ServerSocketChannel.open();
			serverChannel.socket().bind( new InetSocketAddress( host, port ) );
			serverChannel.configureBlocking( false );

			//setup selector to listen for connections on this serverSocketChannel
			serverChannel.register( selector, SelectionKey.OP_ACCEPT );
			
		} catch (IOException e) {
			System.out.println("Configuration for Server failed. Exiting.");
			e.printStackTrace();
			return false;
		}
		System.out.println("Server Successfully configured");

		//start up the ThreadPool
		manager.start();

		//create timer to handle output, and timeout
		timer = new Timer("Output_and_timeout");
		//schedule output
		timer.scheduleAtFixedRate(new ServerOutput(batchTime), Constants.OUTPUT_TIME * 1000,
				Constants.OUTPUT_TIME * 1000);
		timeout = new ScheduleTimeout(this);
		timer.scheduleAtFixedRate(timeout, batchTime * 1000, batchTime * 1000);

		return true;
	}

	private void channelPolling() throws IOException {
		//used by the accept loop to make sure it doesn't attempt to register the same thing multiple times
		Semaphore acceptLock = new Semaphore(1);

		while (true) {
			//Waits until there is activity on one of the registered channels
			if (selector.selectNow() == 0) continue;
			//get list of all keys
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

			while (keys.hasNext()) {
				//get key and find its activity
				SelectionKey key = keys.next();

//				System.out.printf("Key Value: Interest: %d, Ready: %d%n", key.interestOps(), key.readyOps());

				if (!key.isValid()) {
					System.out.println("Canceled key encountered. Ignoring.");
					continue;
				}

				if ( key.isAcceptable()) {

					//Stops redundant task creation. The lock is unlocked as soon as a thread handles
					//the registration of a client. Should never really bottleneck program
					if (!acceptLock.tryAcquire()) {
						continue;
					}

					//Put new AcceptClientConnection in Queue with this key data
					addTask(new AcceptClientConnection(selector, serverChannel, acceptLock, this));

				}

				if ( key.isReadable()) {
					//deregister this key, as it's now not part of the serverSocketChannel, only a specific
					//client. Will reregister read interests as soon as it's read from
					key.interestOps(0);
					//put new ReadClientData in Queue, which this key data
					SocketChannel client = (SocketChannel) key.channel();

					//Make sure the channelsToHandle isn't edited by a thread mid add/size check
					//also synchronized with the Thread's work when it handles the batch organization
					synchronized (channelsToHandle) {
						channelsToHandle.add(new ClientData(client, key));
//						System.out.printf("Client_Data: Appending '%s' to list. Size: %d%n", client.getRemoteAddress(), channelsToHandle.size());

						//if there are more than enough clients to handle, hand it off to a client and move on
						if (channelsToHandle.size() >= batchSize && organizeLock.tryAcquire()) {
							addTask(new OrganizeBatch(channelsToHandle, queue, hashList, batchSize, organizeLock, this));
						}
					}

				}
				if ( key.isWritable()) {
//					System.out.println("Client can be written to");
				}
				//done with the key this iteration, check again for any new activity next select
				keys.remove();
			}
		}
	}
	public void insertClient(String remoteAddress) {
		currentClients.put(remoteAddress, 0);
	}
	public void removeClient(String remoteAddress) {
		if (currentClients.contains(remoteAddress))
			currentClients.remove(remoteAddress);
	}

	//used by the accept task to update the amount of clients
	public void incrementClients() {
		connectedClients.incrementAndGet();
	}

	//used by the catch of the handle if a client disconnects
	public void decrementClients() {
		connectedClients.decrementAndGet();
	}

	//Worker threads call this whenever they get a total
	public void incrementSent() {
		messagesSent.incrementAndGet();
	}

	public void incrementReceived() {
		messagesReceived.incrementAndGet();
	}

	public void incrementClientThroughput(String remoteClient) {
		currentClients.put(remoteClient, currentClients.get(remoteClient) + 1);
	}

	public synchronized void addTask(Task task) {
		timeout.cancel();
		timeout = new ScheduleTimeout(this);
		timer.scheduleAtFixedRate(timeout, batchTime * 1000, batchTime * 1000);
		queue.add(task);
	}



	private class ServerOutput extends TimerTask {
		int batchTime;

		public ServerOutput(int batchTime) {
			this.batchTime = batchTime;
		}

		@Override
		public void run() {
			//store variables as they are rapidly changing in between accesses
			//float messagesPerSecond = messagesReceived.floatValue() / Constants.OUTPUT_TIME;
			int clients = connectedClients.get();
			double standardDeviation = 0;
			double mean = 0;
			Integer total = 0;
			synchronized (currentClients) {
				for (Map.Entry<String, Integer> entry : currentClients.entrySet())
					total += entry.getValue();

				if (clients != 0) {
					mean = total / clients;

					for (Map.Entry<String, Integer> entry : currentClients.entrySet()) {
						String k = entry.getKey();
						Integer v = entry.getValue();

						standardDeviation += Math.pow(v - mean, 2);
						currentClients.put(k, 0);
					}
					standardDeviation = Math.sqrt(standardDeviation / clients - 1);
				}
				else
					mean = 0;
			}

			if (Double.isNaN(standardDeviation))
				standardDeviation = 0;

			System.out.printf("[%s] Server Throughput: %d messages/s, " +
							"Active Client Connections: %d, " +
							"Mean Per-client Throughput: %f messages/s, " +
							"Std. Dev. Of Per-client Throughput: %f messages/s%n",
					new Timestamp(System.currentTimeMillis()),
					total,
					clients,
					mean,
					standardDeviation
					);


			//reset variables for next output
			messagesSent.set(0);
			messagesReceived.set(0);


		}
	}

	private class ScheduleTimeout extends TimerTask {
		Server server;

		public ScheduleTimeout(Server server) {
			this.server = server;
		}

		@Override
		public void run() {
			System.out.println("TIMEOUT REACHED. GIVING TO THREADS");
			synchronized (channelsToHandle) {
				if (channelsToHandle.size() > 0)
					addTask(new OrganizeBatch(channelsToHandle, queue, hashList, channelsToHandle.size(), organizeLock, server));
			}
		}
	}


}
