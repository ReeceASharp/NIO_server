package cs455.scaling.task;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

//is passed the socketChannel
public class AcceptClientConnection implements Task {
	final Selector selector;
	ServerSocketChannel server;
	ArrayList<SocketChannel> clientsToAccept;
	Semaphore lock;
	//SocketChannel channel;

	public AcceptClientConnection(Selector selector, ServerSocketChannel server, Semaphore lock) {
		this.selector = selector;
		this.server = server;
		this.lock = lock;
	}

	public void run() {
		System.out.println(this.getClass().getSimpleName());
		try {
			//pick up the connection to the client
			SocketChannel client = server.accept();
			//register reading interest with the selector, nio
			client.configureBlocking(false);

			client.register(selector, SelectionKey.OP_READ);

			System.out.println("Client successfully registered");
			lock.release();
		} catch ( IOException ioe) {
			ioe.printStackTrace();
		}

//		System.out.println("REturning");
	}

}
