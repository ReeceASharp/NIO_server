package cs455.scaling.client;

import java.util.LinkedList;
import java.util.TimerTask;

import cs455.scaling.util.OutputManager;

public class Client {
	LinkedList<String> hashList;
	//local list of hashed messages
	
	public Client() {
		//OutputManager outputManager = new OutputManager(client);
		hashList = new LinkedList<String>();
	}
	
	public static void main(String[] args) {
		
	}

	public TimerTask print() {
		System.out.println("Got a task");
		return null;
	}
	
}
