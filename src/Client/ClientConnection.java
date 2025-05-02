package Client;

import Common.Message;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientConnection extends Thread {

    SocketAddress socketAddress;
    SocketChannel socketChannel;

    ClientGUI gui;

    ClientConnection(InetAddress address, int port, ClientGUI gui) throws IOException {
        this.socketAddress = new InetSocketAddress(address, port);
        this.socketChannel = SocketChannel.open(socketAddress);
        this.gui = gui;
    }

    public void sendMessage(Message message) throws IOException {
        socketChannel.write(ByteBuffer.wrap(message.getBytes()));
    }

    private boolean closed = false;
    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    @Override
    public void run() {
        try{
            ByteBuffer buffer = ByteBuffer.allocate(120);
            socketChannel.configureBlocking(false);
            while(!closed) {
                try {
                    int n = socketChannel.read(buffer);
                    if (n > 0) {
                        buffer.flip();
                        gui.serverDraw(new Message(buffer.array()));
                        buffer.clear();
                    }
                    else if (n == -1) {
                        break;
                    }
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
