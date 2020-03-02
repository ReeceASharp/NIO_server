package cs455.scaling.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import cs455.scaling.task.AcceptClientConnection;
import cs455.scaling.task.Task;
import cs455.scaling.util.Hasher;
import cs455.scaling.util.OutputManager;

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
	//OutputManager outputManager = new OutputManager();	//don't worry about this right now
	LinkedList<String> hashList;	//stores hashed byte[] received from clients
	private final LinkedBlockingQueue<Task> queue;

	ServerSocketChannel serverSocket;	// the hub for connections from clients to come in on
	Selector selector;					// selects from the available connections

	ThreadPoolManager manager;
	
	public Server(int poolSize, int batchSize, int batchTime) {
		hashList = new LinkedList<String>();
		queue = new LinkedBlockingQueue<>();
		manager = new ThreadPoolManager(poolSize, batchSize, queue);

	}

	public static void main(String[] args) {
		
		//args in form of portnum, thread-pool-size, batch-size, batch-time
		if (args.length != 4) {
			System.out.println("Error. Invalid # of parameters.");
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
			System.out.printf("Host: %s, Port: %d%n", host, port);

			selector = Selector.open();
			serverSocket = ServerSocketChannel.open();

			//setup server socket to listen for connections, but won't handle them
			serverSocket.socket().bind( new InetSocketAddress( "localhost", port ) );
			serverSocket.configureBlocking( false );
			serverSocket.register( selector, SelectionKey.OP_ACCEPT );
			
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
			if (selector.selectNow() == 0) continue;
			//get list of all keys
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

			while (keys.hasNext()) {
				SelectionKey key = keys.next();
				if ( key.isAcceptable()) {
					System.out.println("Got Connection");
					//Put new AcceptClientConnection in Queue with this key data
					queue.add(new AcceptClientConnection(selector, serverSocket));
				}
				if ( key.isReadable()) {
					//put new ReadClientData in Queue, which this key data

				}
				if ( key.isWritable()) {

				}
				keys.remove();
			}
		}
	}
	
	public void receivedData(byte[] dataFromClient) {
		//generate a hash from it and put the
		String hash = Hasher.SHA1FromBytes(dataFromClient);
		hashList.add(hash);

		//p
	}
}
