package cs455.scaling.task;

import cs455.scaling.server.ClientData;

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

    private final int batchSize;

    public OrganizeBatch(ArrayList<ClientData> channelsToHandle, LinkedBlockingQueue<Task> queue,
                         LinkedList<String> hashList, int batchSize, Semaphore lock) {
        this.channelsToHandle = channelsToHandle;
        this.queue = queue;
        this.hashList = hashList;
        this.batchSize = batchSize;
        this.lock = lock;
    }

    @Override
    public void run() {
//        System.out.println(this.getClass().getSimpleName());

        synchronized(channelsToHandle) {
            System.out.printf("Getting list of size: %d from %d %n", batchSize, channelsToHandle.size());
            //System.out.println(channelsToHandle);
            batch = new ArrayList<>(channelsToHandle.subList(0, batchSize));

            if (batchSize > 0) {
                channelsToHandle.subList(0, batchSize).clear();
            }
        }
        lock.release();

        queue.add(new HandleBatch(batch, hashList));
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
