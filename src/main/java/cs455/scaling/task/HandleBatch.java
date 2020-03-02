package cs455.scaling.task;

//this is the message type that tells the Thread to take all of the data
public class HandleBatch implements Task {

	public void run() {
		System.out.println(this.getClass().getSimpleName());
	}

	public int getType() {
		return 0;
	}

}
