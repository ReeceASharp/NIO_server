package cs455.scaling.task;

import cs455.scaling.server.ClientData;
import cs455.scaling.server.Server;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class OrganizeBatch implements Task {
    private ArrayList<ClientData> channelsToHandle;
    private ArrayList<ClientData> batch;
    private LinkedBlockingQueue<Task> queue;
    private LinkedList<String> hashList;
    private Semaphore lock;
    private Server server;

    private final int batchSize;

    //A task sent to threads to organize the current data in the queue into a data structure
    //that a task can handle. This is all passing by reference, so task creation isn't too
    //much of a hassle for the selection thread
    public OrganizeBatch(ArrayList<ClientData> channelsToHandle, LinkedBlockingQueue<Task> queue,
                         LinkedList<String> hashList, int batchSize, Semaphore lock, Server server) {
        this.channelsToHandle = channelsToHandle;
        this.queue = queue;
        this.hashList = hashList;
        this.batchSize = batchSize;
        this.lock = lock;
        this.server = server;
    }

    @Override
    public void run() {
        //create a deep-copy of the list of work of batchSize size, but can also be smaller, like when
        //timeout is hit. When it's done dealing with data, release the queue
        synchronized(channelsToHandle) {
//          System.out.printf("Getting list of size: %d from %d %n", batchSize, channelsToHandle.size());

            if (batchSize > 0) {
                batch = new ArrayList<>(channelsToHandle.subList(0, batchSize));
                channelsToHandle.subList(0, batchSize).clear();
            }
        }
        lock.release();

        server.addTask(new HandleBatch(batch, hashList, server));
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
