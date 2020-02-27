package cs455.scaling.task;

public class AcceptClientConnection implements Task {

	public void run() {
		System.out.println(this.getClass().getSimpleName());
	}

	public int getType() {
		return 0;
	}

}
