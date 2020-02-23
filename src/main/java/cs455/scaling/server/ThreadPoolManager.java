package cs455.scaling.server;

import java.util.concurrent.LinkedBlockingQueue;

public class ThreadPoolManager {
	
	private final int poolSize;
    private final ThreadPoolWorker[] workers;
    private final LinkedBlockingQueue<Runnable> queue;
    
    public ThreadPoolManager(int poolSize) {
    	
        this.poolSize = poolSize;
        workers = new ThreadPoolWorker[poolSize];
        
        queue = new LinkedBlockingQueue();
        
        for (int i = 0; i < poolSize; i++) {
            workers[i] = new ThreadPoolWorker();
            workers[i].start();
        }
	}
    
    public void execute(Runnable task) {
        synchronized (queue) {
            queue.add(task);
            queue.notify();
        }
    }

}
