package cs455.scaling.task;

import cs455.scaling.server.Server;

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
	ServerSocketChannel serverChannel;
	ArrayList<SocketChannel> clientsToAccept;
	Semaphore lock;
	Server server;

	public AcceptClientConnection(Selector selector, ServerSocketChannel serverChannel, Semaphore lock, Server server) {
		this.selector = selector;
		this.serverChannel = serverChannel;
		this.lock = lock;
		this.server = server;
	}

	public void run() {
		SocketChannel client;
		try {
			//pick up the connection to the client
			client = serverChannel.accept();

			if (client == null) {
				lock.release();
				return;
			}

			//register reading interest with the selector, nio
			client.configureBlocking(false);

			//register this channel with the selector to pay attention when it can read
			client.register(selector, SelectionKey.OP_READ);
			server.insertClient(client.getRemoteAddress().toString());
			System.out.printf("Client successfully registered: %s%n", client.getRemoteAddress());
		} catch ( IOException ioe) {
			ioe.printStackTrace();
		}
		//increment the active client client count, and allow another client to register with the server
		server.incrementClients();

		lock.release();
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}
}
