package cs455.scaling.task;

import cs455.scaling.server.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


//this is a task utilized by the Threadpool to on the assumption that a Client is sending
//some data, in this case a byte[] to the client.
public class ReadClientData implements Task {
	SocketChannel channel;	//Connection that data is being read from
	Server server;

	public ReadClientData(SocketChannel channel, Server server) {
		this.channel = channel;
		this.server = server;
	}

	public void run() {
		System.out.println(this.getClass().getSimpleName());

		//attempt to lock down this channel with an attachment

		//read until all 8kb is read, hardcoded
		ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
		int bytesRead = 0;
		try {
			while (buffer.hasRemaining() && bytesRead != -1) {
				bytesRead = channel.read(buffer);
			}
		} catch (IOException ioe) {
			System.out.println("Error reading from buffer.");
		}

		buffer.rewind();
		//update server with readData
		server.receivedData(buffer.array());
	}


}
