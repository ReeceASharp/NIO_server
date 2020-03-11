package cs455.scaling.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import cs455.scaling.task.Constants;
import cs455.scaling.util.Hasher;

public class Client {
	private final Random byteGenerator = new Random();	//non-static as multithreaded could cause contention

	private String serverHost;				//local host name of server
	private int serverPort;					// port of server
	private int messageRate;				// 1 / messageRate messages generated per second
	final private LinkedList<String> hashList;	// local list of hashed messages

	//message statistics
	private AtomicInteger messagesSent;
	private AtomicInteger messagesReceived;

	private SocketChannel serverChannel;

	private Timer timer;

	
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
			client.serverConnect();
		} catch (IOException io) {
			System.out.printf("Unable to connect to: %s:%d%n", serverHost, serverPort);
			return;
		}


		client.start();

		//start generating and sending byte[]
		//client.generateMessages();
//		System.out.println("Client is Exiting.");
	}

	private void start() {

		timer = new Timer("");
		timer.scheduleAtFixedRate(new SendData(), 0, 1000 / messageRate);
		timer.scheduleAtFixedRate(new ClientDisplay(), Constants.OUTPUT_TIME * 1000, Constants.OUTPUT_TIME * 1000);
		//Start listening for incoming data
		listen();
	}

	private void listen() {
		ByteBuffer hashReceive = ByteBuffer.allocate(Constants.HASH_SIZE);
		while (true) {
			int bytesRead = 0;
			try {
				while (hashReceive.hasRemaining() && bytesRead != -1) {
//					System.out.println("Waiting for response");
					bytesRead = serverChannel.read(hashReceive);
				}
			} catch (IOException ioe) {
				System.out.println("Error reading from buffer.");

			}

			if (bytesRead == -1) {
				timer.cancel();

				try {
					serverChannel.close();
				} catch (IOException e) { /* ignore */ }

				return;
			}

			String receivedHash = new String(hashReceive.array());

			synchronized (hashList) {
				int index = hashList.indexOf(receivedHash);
				if (index > -1) {
//					System.out.println("REMOVING: \t\t" + hashList.get(index));
					hashList.remove(index);
					messagesReceived.incrementAndGet();
				} else {
					System.out.printf("Error: Did not find '%s' in generated hashes.%n", receivedHash);
				}
			}

//			System.out.printf("Received Message: \t  '%s' Size: %d%n%n", receivedHash, bytesRead);

			//reset for next message
			hashReceive.clear();
		}
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

	//A small class that gives the current client data along with a timestamp
	//utilized on the same Timer as the message sending
	private class ClientDisplay extends TimerTask {
		public ClientDisplay() { }

		@Override
		public void run() {
			System.out.printf("[%s] Total Sent Count: %d, Total Received Count: %d%n",
					new Timestamp(System.currentTimeMillis()), messagesSent.get(),
					messagesReceived.get());

			//reset counters for next timestamp output
			messagesReceived.set(0);
			messagesSent.set(0);

		}
	}

	//utilized by the Client to send data 1 / messageRate times per second
	private class SendData extends TimerTask {
		byte[] dataToSend;
		ByteBuffer buffer;

		public SendData() {
			dataToSend = new byte[Constants.BUFFER_SIZE];

			buffer = ByteBuffer.wrap(dataToSend);
		}

		@Override
		public void run() {
			try {
				//generate a random byte[]
				byteGenerator.nextBytes(dataToSend);
				//Hash and append to linkedList
				String hash = Hasher.SHA1FromBytes(dataToSend);
//				System.out.printf("Generated Message with hash: %s%n", hash);

				//write message over buffer
				while (buffer.hasRemaining()) {
//					System.out.println("Writing");
					serverChannel.write(buffer);
				}

				messagesSent.incrementAndGet();
				//written successfully, append to linked list
				synchronized (hashList) {
					hashList.add(hash);
				}

				//reset buffer to beginning, so it can write new information next time
				buffer.clear();
			} catch (IOException ioe) {
				System.out.println("Error writing to server channel. Exiting.");
				this.cancel();
			}
		}
	}
}
