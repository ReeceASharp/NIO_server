package cs455.scaling.server;

import cs455.scaling.task.Task;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

//utilized by the ThreadPoolManager in a ThreadPool
public class ThreadPoolWorker extends Thread {
	//somehow keep track of tasks

	//local reference to FIFO queue in this thread's server
	private final LinkedBlockingQueue<Task> queue;
	
	public ThreadPoolWorker(LinkedBlockingQueue<Task> queue) {
		this.queue = queue;
	}

	@Override
	public void run() {

		//This needs to run the task given
		try {
			while (true) {
				Task task = queue.take();
//				System.out.println("Worker is starting Task: " + task.getName());
				task.run();


			}
		} catch (InterruptedException ioe) {
			System.out.println("Worker is exiting Thread.");
		}
	}
}
