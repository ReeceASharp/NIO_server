package cs455.scaling.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.LinkedList;

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
	LinkedList<String> hashList;
	
	ServerSocketChannel serverSocket;
	Selector selector;
	//OutputManager outputManager = new OutputManager();
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
		
		int port = Integer.parseInt(args[0]);
		int poolSize = Integer.parseInt(args[1]);
		int batchSize = Integer.parseInt(args[2]);
		int batchTime = Integer.parseInt(args[3]);
		
		
		Server server = new Server(poolSize, batchSize, batchTime);
		server.configureAndStart(port);
		
		
	}
	
	private void configureAndStart(int port) {
		try {
			selector = Selector.open();
			serverSocket = ServerSocketChannel.open();
			
			//get local host
			String host = InetAddress.getLocalHost().getHostName();
			
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
	
	
	
	
}
