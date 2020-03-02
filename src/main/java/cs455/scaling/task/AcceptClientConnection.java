package cs455.scaling.task;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

//is passed the socketChannel
public class AcceptClientConnection implements Task {
	Selector selector;
	ServerSocketChannel serverSocket;
	//SocketChannel channel;

	public AcceptClientConnection(Selector selector, ServerSocketChannel serverSocket) {
		this.selector = selector;
		this.serverSocket = serverSocket;
	}

	public void run() {
		System.out.println(this.getClass().getSimpleName());

		try {
			SocketChannel client = serverSocket.accept();

			client.configureBlocking( false );
			client.register(selector, SelectionKey.OP_READ);

		} catch ( IOException ioe) {
			ioe.printStackTrace();
		}

	}

	public int getType() {
		return 0;
	}

}
