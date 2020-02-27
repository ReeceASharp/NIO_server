package cs455.scaling.server;

import java.util.concurrent.LinkedBlockingQueue;

import cs455.scaling.task.Task;

public class ThreadPoolManager {
	
	private final int poolSize;
    private final ThreadPoolWorker[] workers;
    private final LinkedBlockingQueue<Task> queue;
    
    public ThreadPoolManager(int poolSize, int batchSize) {
    	
    	//start 
        this.poolSize = poolSize;
        workers = new ThreadPoolWorker[poolSize];
        
        queue = new LinkedBlockingQueue<Task>(batchSize);
        
        for (int i = 0; i < poolSize; i++) {
            workers[i] = new ThreadPoolWorker();
        }
	}
    
    public void start() {
        for (int i = 0; i < poolSize; i++) {
            workers[i].start();
        }
    }
    
    public void execute(Task task) {
        synchronized (queue) {
            queue.add(task);
            queue.notify();
        }
    }

    //Look into queue.take()
    //
    
}
