package cs455.scaling.server;

import java.util.concurrent.LinkedBlockingQueue;

import cs455.scaling.task.Task;

public class ThreadPoolManager {
	
	private final int poolSize;
    private final ThreadPoolWorker[] workers;
    private final LinkedBlockingQueue<Task> queue;
    
    public ThreadPoolManager(int poolSize, int batchSize, LinkedBlockingQueue<Task> queue) {
    	
    	//start 
        this.poolSize = poolSize;
        workers = new ThreadPoolWorker[poolSize];
        this.queue = queue;
        
        for (int i = 0; i < poolSize; i++) {
            workers[i] = new ThreadPoolWorker(queue);
        }
	}
    
    public void start() {
        for (int i = 0; i < poolSize; i++) {
            workers[i].start();
        }
    }

    //Look into queue.take()
    //
    
}
