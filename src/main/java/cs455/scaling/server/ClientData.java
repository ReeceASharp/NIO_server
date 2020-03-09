package cs455.scaling.server;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

public class ClientData {
    //store the client socket
    //TODO: Find out how to get socketChannel

    private SocketChannel channel;

    //store the byte[]

    //store the SHA1


    public ClientData(SocketChannel channel) {
        this.channel = channel;
    }
}
