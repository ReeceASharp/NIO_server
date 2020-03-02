package cs455.scaling.task;

import java.nio.channels.SocketChannel;

//is passed the socketChannel
public class AcceptClientConnection implements Task {
	SocketChannel channel;

	public AcceptClientConnection(SocketChannel channel) {
		this.channel = channel;
	}

	public void run() {
		System.out.println(this.getClass().getSimpleName());

	}

	public int getType() {
		return 0;
	}

}
