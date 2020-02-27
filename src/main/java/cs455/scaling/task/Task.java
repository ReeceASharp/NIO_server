package cs455.scaling.task;

// each task passed to the ThreadPool is something that can be
// more generalized, but needs to be something that a thread can run
public interface Task extends Runnable {
	public int getType();
}
