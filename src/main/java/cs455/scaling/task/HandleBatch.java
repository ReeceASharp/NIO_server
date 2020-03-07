package cs455.scaling.task;

import cs455.scaling.server.Batch;
import cs455.scaling.server.ClientData;

//this is the message type that gives the thread a 'batch' of data to handle
public class HandleBatch implements Task {
	private Batch data;

	public HandleBatch(Batch data) {
		this.data = data;
	}

	public void run() {
		System.out.println(this.getClass().getSimpleName());

		//For each byte[] passed in the batch, calculate the SHA1, and attempt to pass it to the
		//Server.
		for (ClientData cd : data) {

		}


	}

}
