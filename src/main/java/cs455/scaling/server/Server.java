package cs455.scaling.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

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
	LinkedList<String> hashList;

	ServerSocketChannel serverSocket;
	Selector selector;

	ThreadPoolManager manager;
	
	public Server(int poolSize, int batchSize, int batchTime) {
		hashList = new LinkedList<String>(); 
		manager = new ThreadPoolManager(poolSize, batchSize);
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
			serverSocket.socket().bind( new InetSocketAddress( host, port ) );
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
		while (true) {
			//no Clients connected
			if (selector.selectNow() == 0) continue;

			//get list of all keys
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				SelectionKey key = keys.next();
				if ( key.isAcceptable()) {
					//key.
					//Put new AcceptClientConnection in Queue with this key data
				}
				if ( key.isReadable()) {
					//put new ReadClientData in Queue, which this key data

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
