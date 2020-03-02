package cs455.scaling.server;

//utilized by the ThreadPoolManager in a ThreadPool
public class ThreadPoolWorker extends Thread {
	//somehow keep track of tasks

	
	public ThreadPoolWorker() {

	}

	@Override
	public void run() {
		System.out.println("Running a ThreadPoolWorker");

		//This needs to run the task given

		//super.run();
	}
}
