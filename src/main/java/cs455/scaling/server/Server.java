package cs455.scaling.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import cs455.scaling.task.AcceptClientConnection;
import cs455.scaling.task.HandleBatch;
import cs455.scaling.task.OrganizeBatch;
import cs455.scaling.task.Task;
import cs455.scaling.util.Hasher;

/*
1.1 Server Node:
There is exactly one server node in the system. The server node provides the following functions:
A. Accepts incoming network connections from the clients.
B. Accepts incoming traffic from these connections
C. Groups data from the clients together into batches
D. Replies to clients by sending back a hash code for each message received.
E. The server performs functions A, B, C, and D by relying on the thread pool. 
*/

// Idea: use an intermediary data-structure to handle the batches.
// Connections should be immediately put into the queue, as it
// wouldn't make sense to have them handle

public class Server {
	//OutputManager outputManager = new OutputManager();	//don't worry about this right now

	private LinkedList<String> hashList;	//stores hashed byte[] received from clients
	private final LinkedBlockingQueue<Task> queue;	//stores entire tasks to handle
	private final ArrayList<ClientData> channelsToHandle;		//Stores the clients with data to handle
	private final ArrayList<SocketChannel> clientsToAccept;

	//TODO: need to do something to keep track of per client messaging rates
	//private final ArrayList<>

	private final int poolSize;
	private final int batchSize;
	private final int batchTime;


	private ServerSocketChannel server;	// the hub for connections from clients to come in on
	private Selector selector;					// selects from the available connections

	private ThreadPoolManager manager;
	
	public Server(int poolSize, int batchSize, int batchTime) {
		hashList = new LinkedList<String>();
		queue = new LinkedBlockingQueue<>();
		channelsToHandle = new ArrayList<>();
		clientsToAccept = new ArrayList<>();

		this.poolSize = poolSize;
		this.batchSize = batchSize;
		this.batchTime = batchTime;

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

		System.out.printf("BATCHSIZE: %d, BATCHTIME: %d%n", batchSize, batchTime);
		
		//initialize the server
		Server server = new Server(poolSize, batchSize, batchTime);

		//setup the serverSocket to listen to incoming connections, and startup the threadPool
		server.configureAndStart(port);
		System.out.println("Server Successfully configured");

		try {
			server.channelPolling();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}
	
	private void configureAndStart(int port) {
		//setup the server NIO
		try {
			//get local host to bind
			String host = InetAddress.getLocalHost().getHostName();
			host = "localhost";
			System.out.printf("Host: %s, Port: %d%n", host, port);

			selector = Selector.open();

			server = ServerSocketChannel.open();
			server.socket().bind( new InetSocketAddress( host, port ) );
			server.configureBlocking( false );

			//setup selector to listen for connections on this serverSocketChannel
			server.register( selector, SelectionKey.OP_ACCEPT );


			
		} catch (IOException e) {
			System.out.println("Configuration for Server failed. Exiting");
			e.printStackTrace();
		}

		//start up the ThreadPool
		manager.start();
	}


	
	private void channelPolling() throws IOException {
		//used by the accept loop to make sure it doesn't attempt to register the same thing multiple times
		Semaphore acceptLock = new Semaphore(1);
		Semaphore organizeLock = new Semaphore(1);
		System.out.println("Listening...");
		while (true) {
			//Blocks until there is activity on one of the channels

//			System.out.print("+");
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}

//			SelectionKey.OP_ACCEPT = 16
//			SelectionKey.OP_CONNECT = 8
//			SelectionKey.OP_READ = 1
//			SelectionKey.OP_WRITE = 4

//			System.out.println("Waiting For Activity");
//			if (selector.selectNow() == 0) continue;
			selector.select();
//			System.out.println("ENDING BLOCK");
			//get list of all keys
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

			while (keys.hasNext()) {

				//get key and find its activity
				SelectionKey key = keys.next();



				System.out.printf("Key Value: Interest: %d, Ready: %d%n", key.interestOps(), key.readyOps());

				if (!key.isValid()) {
					System.out.println("Canceled key encountered. Ignoring.");
					continue;
				}

				if ( key.isAcceptable()) {

					if (!acceptLock.tryAcquire()) {
						System.out.println("Lock Already Initiated");
						continue;
					}


					//already created a task for it

					System.out.printf("Accepting New Connection. %s%n", key.channel());

					//Put new AcceptClientConnection in Queue with this key data
					queue.add(new AcceptClientConnection(selector, server, acceptLock));
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}



				if ( key.isReadable()) {

					//deregister this key, as it's now not part of the serverSocketChannel, only a specific
					//client
					key.interestOps(0);
					//put new ReadClientData in Queue, which this key data
					SocketChannel client = (SocketChannel) key.channel();

					//Make sure the channelsToHandle isn't edited by a thread mid add/size check
					//also synchronized with the Thread's work when it handles the batch organization
					synchronized (channelsToHandle) {
						channelsToHandle.add(new ClientData(client, key));
						System.out.printf("Client sent Data. Appending '%s' to list. Current Size: %d%n", client.getRemoteAddress(), channelsToHandle.size());

						//if there are more than enough clients to handle, hand it off to a client and move on
						if (channelsToHandle.size() >= batchSize && organizeLock.tryAcquire()) {
							System.out.println("BATCH LIMIT REACHED. Organizing data.");
							queue.add(new OrganizeBatch(channelsToHandle, queue, hashList, batchSize, organizeLock));
						}
					}

//					//channelsToHandle.add((SocketChannel) key.channel());
//					ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
//
//					System.out.printf("Client has data to read: Remaining: '%d' %s %n", buffer.remaining(), client.getLocalAddress());
//					int bytesRead = 0;
//					try {
//						//buffer is empty, or a full set of data has been read
//						while (buffer.hasRemaining() && bytesRead != -1) {
//							System.out.println("Reading");
//							bytesRead = client.read(buffer);
//						}
//					} catch (IOException ioe) {
//						key.channel().close();
//						key.cancel();
//						System.out.println("Error reading from buffer. Removing Client");
//
//						//Deregister this client, cancel the key, and move on
//						//Note: when deregistering a client, when it is shut down remotely it'll
//						//activate read-interest on the selector to read a potential error. Ignore it.
//						continue;
//					}
//
//					//Successful read, convert received message to Hash, store, and send back
//					String hash = Hasher.SHA1FromBytes(buffer.array());
//					hashList.add(hash);
////					String message = new String (buffer.array()).trim();
//					System.out.printf("Message: '%s' Size: %d%n", hash, bytesRead);


					//deregister this so the task isn't constantly put into the queue
					//it'll be reregisterd with read interests as soon as the data is read from it


//					if (clientDataList.size() > batchSize) {
//						queue.add(new OrganizeBatch(clientDataList, queue, batchSize));
//					}

				}
				if ( key.isWritable()) {
					System.out.println("Client can be written to");
				}
//				System.out.println("Removing key");
				keys.remove();
			}
//			System.out.println("RAN OUT OF KEYS");
		}
	}
	
	public synchronized void receivedData(byte[] dataFromClient) {
		//generate a hash from it and put the
		String hash = Hasher.SHA1FromBytes(dataFromClient);
		hashList.add(hash);

		//p
	}

	public synchronized void cancelKey(SocketChannel channelToCancel) {
		//selector.selectedKeys().
	}
}
