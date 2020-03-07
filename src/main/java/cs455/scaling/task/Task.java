package cs455.scaling.task;

// each task passed to the ThreadPool is something that can be
// more generalized, but needs to be something that a thread can run

/*
	Task Ideas:
		Register a client
		Add a batch of readable data to the queue
		Take batch
 */

public interface Task extends Runnable {
}
