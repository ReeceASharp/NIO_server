package cs455.scaling.server;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ClientData {

    public final SocketChannel channel;
    public final SelectionKey key;

    public ClientData(SocketChannel channel, SelectionKey key) {
        this.channel = channel;
        this.key = key;
    }
}
