package cs455.scaling.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import cs455.scaling.task.Constants;
import cs455.scaling.util.Hasher;
import cs455.scaling.util.OutputManager;

public class Client {
	private final Random byteGenerator = new Random();	//non-static as multithreaded could cause contention

	private String serverHost;				//local host name of server
	private int serverPort;					// port of server
	private int messageRate;				// 1 / messageRate messages generated per second
	private LinkedList<String> hashList;	// local list of hashed messages

	//message statistics
	private AtomicInteger messagesSent;
	private AtomicInteger messagesReceived;

	private SocketChannel serverChannel;

	
	public Client(String serverHost, int serverPort, int messageRate) {
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		this.messageRate = messageRate;
		this.hashList = new LinkedList<>();

		this.messagesSent = new AtomicInteger();
		this.messagesReceived = new AtomicInteger();
	}
	
	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Invalid # of arguments. (HOST SERVERPORT MESSAGERATE)");
			return;
		}

		//pull arguments from command line
		String serverHost = args[0];
		int serverPort = Integer.parseInt(args[1]);
		int messageRate = Integer.parseInt(args[2]);

		//create client, and attempt to connect to server
		Client client = new Client(serverHost, serverPort, messageRate);

		try {
			//to give the server a second to setup before this starts
			Thread.sleep(100);

			client.serverConnect();
		} catch (IOException | InterruptedException ioe) {
			System.out.printf("Unable to connect to: %s:%d%n", serverHost, serverPort);
			return;
		}

		//start generating and sending byte[]
		client.generateMessages();

		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Client is Exiting.");
	}

	//Client is attempting to connect to the server, socketChannels can throw errors if the server
	//isn't up and running on specified host:port
	private void serverConnect() throws IOException {
		System.out.printf("Attempting to connect to: %s:%s%n", serverHost, serverPort);
		//create a channel, non-blocking, and attempt to connect to the server
		serverChannel = SocketChannel.open();
		serverChannel.connect(new InetSocketAddress(serverHost, serverPort));
		System.out.println("Connected To server via: " + serverChannel.getLocalAddress());
	}

	private void generateMessages() {
		System.out.println("Starting Message Generation...");
		byte[] dataToSend = new byte[Constants.BUFFER_SIZE];
		ByteBuffer hashReceive = ByteBuffer.allocate(40);
		ByteBuffer buffer = ByteBuffer.wrap(dataToSend);
		try {
			for (int i = 0; i < 10; i++ ) {
//			while (true) {
				//generate a random byte[]
				byteGenerator.nextBytes(dataToSend);
				//Hash and append to linkedList
				String hash = Hasher.SHA1FromBytes(dataToSend);
				System.out.printf("Generated Message with hash: %s%n", hash);
				//write message over buffer

				while (buffer.hasRemaining()) {
					//System.out.println("Writing");
					serverChannel.write(buffer);
				}

				//written successfully, append to linked list
				hashList.add(hash);

				//reset buffer to beginning, so it can write new information next time
				buffer.clear();

				int bytesRead = 0;
				try {
					while (hashReceive.hasRemaining() && bytesRead != -1) {
						//System.out.println("Waiting for response");
						bytesRead = serverChannel.read(hashReceive);
					}
				} catch (IOException ioe) {
					System.out.println("Error reading from buffer.");
				}



				String receivedHash = new String(hashReceive.array());
				System.out.printf("Received Message:  '%s' Size: %d%n%n", receivedHash, bytesRead);

				//reset for next message
				hashReceive.clear();



				//System.out.printf("Sent Message. Waiting %.3f seconds.%n", (float) 1 / messageRate);
				Thread.sleep(1000 / messageRate);
			}
		} catch (InterruptedException ie) {
			System.out.println("Message Loop Canceled. Exiting.");
		} catch (IOException ioe) {
			System.out.println("Error writing to server channel. Exiting.");
		}
		System.out.println("Exiting.");
	}


	//used to print out client data each second
	public TimerTask print() {
		System.out.println("Got a task");
		return null;
	}
	
}
