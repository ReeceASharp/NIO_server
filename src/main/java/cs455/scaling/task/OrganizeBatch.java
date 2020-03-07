package cs455.scaling.task;

import cs455.scaling.server.ClientData;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class OrganizeBatch implements Task {
    private ArrayList<ClientData> clientDataList;
    private LinkedBlockingQueue<Task> queue;
    private final int batchSize;

    public OrganizeBatch(ArrayList<ClientData> clientDataList, LinkedBlockingQueue<Task> queue, int batchSize) {
        this.clientDataList = clientDataList;
        this.queue = queue;
        this.batchSize = batchSize;
    }

    @Override
    public void run() {
        System.out.println(this.getClass().getSimpleName());

        ClientData[] batch = (ClientData[]) clientDataList.subList(0, batchSize-1).toArray();

    }
}
