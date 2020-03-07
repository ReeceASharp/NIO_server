package cs455.scaling.server;

import java.util.Iterator;

//Stored inside the handleBatch task for a client to iterate through
public class Batch implements Iterable<ClientData> {
    ClientData[] clientsToHandle;

    public Batch(ClientData[] clientsToHandle) {
        this.clientsToHandle = clientsToHandle;
    }


    @Override
    public Iterator<ClientData> iterator() {
        //TODO
        return null;
    }
}
