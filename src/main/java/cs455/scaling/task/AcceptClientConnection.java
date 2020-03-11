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
//		System.out.println(this.getClass().getSimpleName());
		try {
			//pick up the connection to the client
			SocketChannel client = server.accept();

			if (client == null) {
//				System.out.println("Client is Null *****************");
				lock.release();
				return;
			}

			//register reading interest with the selector, nio
			client.configureBlocking(false);

			//register this channel with the selector to pay attention when it can read
			client.register(selector, SelectionKey.OP_READ);
			//client.finishConnect();

//			while(client.isConnectionPending()) {
//				System.out.print("PENDING");
//			}
//			while(!client.isConnected()) {
//				System.out.println("NOT CONNECTED");
//			}

//			System.out.printf("Client successfully registered: %s%n", client.getRemoteAddress());
		} catch ( IOException ioe) {
			ioe.printStackTrace();
		}
		lock.release();
//		System.out.println("Releasing Lock and returning\n");

	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}
}
