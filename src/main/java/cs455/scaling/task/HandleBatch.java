package cs455.scaling.task;

import cs455.scaling.server.Batch;
import cs455.scaling.server.ClientData;
import cs455.scaling.util.Hasher;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

//this is the message type that gives the thread a 'batch' of data to handle
public class HandleBatch implements Task {
	//private Batch data;

	private final ArrayList<ClientData> channelsToHandle;		//Stores the clients with data to handle
	private LinkedList<String> hashList;
	private int batchSize;	//Can either be the max batch size, or it'll be the size of the batch if it hit timeout

	public HandleBatch(ArrayList<ClientData> channelsToHandle, LinkedList<String> hashList, int batchSize) {
		this.channelsToHandle = channelsToHandle;
		this.hashList = hashList;
		this.batchSize = batchSize;


	}

	public void run() {
		System.out.println(this.getClass().getSimpleName());
		//preallocate the buffer, and then just rewind it each time
		ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);


		ArrayList<ClientData> batch;

		//get the current batch from the intermediate list
		//read each
		synchronized(channelsToHandle) {
			System.out.printf("Getting list of size: %d%n", batchSize);
			//System.out.println(channelsToHandle);
			batch = new ArrayList<>(channelsToHandle.subList(0, batchSize));

			if (batchSize > 0) {
				channelsToHandle.subList(0, batchSize).clear();
			}
		}


		//For each byte[] passed in the batch, calculate the SHA1, and attempt to pass it to the
		//Server.
		for (ClientData client : batch) {
			try {
				int bytesRead = 0;

				//buffer is empty, or a full set of data has been read
				System.out.printf("Client has data to read: Remaining: '%d' %s%n", buffer.remaining(), client.channel.getRemoteAddress());
				while (buffer.hasRemaining() && bytesRead != -1) {
					System.out.println("Reading");
					bytesRead = client.channel.read(buffer);
				}

				//Successful read, convert received message to Hash, store, and send back
				String hash = Hasher.SHA1FromBytes(buffer.array());
				hashList.add(hash);
//					String message = new String (buffer.array()).trim();
				System.out.printf("Message: '%s' Size: %d%n", hash, hash.getBytes().length);

				ByteBuffer hashToSend = ByteBuffer.wrap(hash.getBytes());


				while (hashToSend.hasRemaining()) {
					System.out.printf("Writing back to Client: '%s'%n", hash);
					client.channel.write(hashToSend);
				}

				hashToSend.rewind();
				buffer.rewind();

				//Server parsed data from client, listen for more
				client.key.interestOps(SelectionKey.OP_READ);

			} catch (IOException ioe) {
//				key.channel().close();
//				key.cancel();
				System.out.println("Error reading from buffer. Removing Client");

				//Deregister this client, cancel the key, and move on
				//Note: when deregistering a client, when it is shut down remotely it'll
				//activate read-interest on the selector to read a potential error. Ignore it.
			}

		}


	}

}
