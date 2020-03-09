package cs455.scaling.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import cs455.scaling.task.Constants;
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
	LinkedList<String> hashList;	//stores hashed byte[] received from clients
	private final LinkedBlockingQueue<Task> queue;	//stores entire tasks to handle
	private final ArrayList<ClientData> clientDataList;		//Stores the clients with data to handle

	private final int poolSize;
	private final int batchSize;
	private final int batchTime;

	ServerSocketChannel server;	// the hub for connections from clients to come in on
	Selector selector;					// selects from the available connections
/
	ThreadPoolManager manager;
	
	public Server(int poolSize, int batchSize, int batchTime) {
		hashList = new LinkedList<String>();
		queue = new LinkedBlockingQueue<>();
		clientDataList = new ArrayList<>();

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
			server.register( selector, SelectionKey.OP_ACCEPT );
			//setup server socket to listen for connections, but won't handle them


			
		} catch (IOException e) {
			System.out.println("Configuration for Server failed. Exiting");
			e.printStackTrace();
		}

		//start up the ThreadPool
		manager.start();
	}
	
	private void channelPolling() throws IOException {
		System.out.println("Listening...");
		while (true) {
			//no Clients connected
//			if (selector.selectNow() == 0) continue;
			selector.select();
			//get list of all keys
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

			while (keys.hasNext()) {
				//get key and find its activity
				SelectionKey key = keys.next();

				if ( key.isAcceptable()) {
					System.out.printf("Got Connection. %s%n", key);
					//unregister the interest to accept, and create task to handle reading
					//key.interestOps(0);

					//System.out.println("New key interests: " + key.interestOps());
					//Put new AcceptClientConnection in Queue with this key data
					//queue.add(new AcceptClientConnection(selector, serverSocket));

					//pick up connection
					SocketChannel client = server.accept();

					//register reading interest with the selector, but don't worry about blocking
					client.configureBlocking(false);
					client.register(selector, SelectionKey.OP_READ);
					System.out.println("Client Registered");

				}

				if ( key.isReadable()) {
					//put new ReadClientData in Queue, which this key data
					SocketChannel client = (SocketChannel) key.channel();
					System.out.println("Client has data to read: " + client);
					//clientDataList.add(new ClientData(clientConnection));

					ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
					int bytesRead = 0;
					try {
						//buffer is empty, or a full set of data has been read
						while (buffer.hasRemaining() && bytesRead != -1) {
							System.out.print("+");
							bytesRead = client.read(buffer);
						}
					} catch (IOException ioe) {
						System.out.println("Error reading from buffer.");
					}

					String hash = Hasher.SHA1FromBytes(buffer.array());
//					String message = new String (buffer.array()).trim();
					System.out.printf("Message: '%s' Size: %d%n", hash, bytesRead);



					//buffer.clear();
//					if (clientDataList.size() > batchSize) {
//						queue.add(new OrganizeBatch(clientDataList, queue, batchSize));
//					}
				}
				if ( key.isWritable()) {
					System.out.println("Client can be written to");
				}
				System.out.println("Removing key");
				keys.remove();
			}
			System.out.println("RAN OUT OF KEYS");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void receivedData(byte[] dataFromClient) {
		//generate a hash from it and put the
		String hash = Hasher.SHA1FromBytes(dataFromClient);
		hashList.add(hash);

		//p
	}
}
