package cs455.scaling.util;

import java.util.Timer;
import java.util.TimerTask;

import cs455.scaling.client.Client;

/*
 * Similarly, once every 20 seconds after starting up, every client should print the number of messages it
 * has sent and received during the last 20 seconds. This log message should look similar to the following.
 * '[timestamp] Total Sent Count: x, Total Received Count: y'
 */

//handles display of information regarding the client
public class OutputManager {	
	
	
	public OutputManager(Client client) {
		//this.client = client;
		
		
		System.out.println("Printing details every 20 seconds");
		Timer timer = new Timer();
		
		Task task = new Task();
		
		//20 seconds
		timer.scheduleAtFixedRate(task, 0, 1000 * 20);
	} 
	
	public class clientOutput {
		
	}
	
	public class ServerOutput {
		
	}
	
}


class Task extends TimerTask {

	
	@Override
	public void run() {
		System.out.println("Printing Task");
	}
	
}