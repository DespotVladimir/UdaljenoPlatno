package Server;

import Common.Message;
import Common.Room;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class Server {

    GraphicsContext gc;

    public static void main(String[] args)  {
        Canvas c = new Canvas();
        GraphicsContext gc = c.getGraphicsContext2D();
        List<Room> rooms = new ArrayList<>();
        Selector selector = null;
        try {
            selector = Selector.open();
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(12345));
            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.err.println("Server started");
            HashSet<SocketChannel> clients = new HashSet<>();
            String leftover="";
            while (true) {
                selector.select();

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();
                ByteBuffer buffer = ByteBuffer.allocate(1024);

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    try{
                        if (key.isAcceptable()) {
                            ServerSocketChannel srv = (ServerSocketChannel) key.channel();
                            SocketChannel client = srv.accept();
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE);
                            clients.add(client);
                            System.err.println("Client accepted. " + client.getRemoteAddress());

                        }
                        else if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            int read = client.read(buffer);
                            if (read > 0) {
                                buffer.flip();
                                byte[] datas = new byte[buffer.limit()];
                                buffer.get(datas);
                                String data = leftover + new String(datas);
                                leftover="";
                                String[] lines = data.split("\n");
                                for(String line: lines)
                                {
                                    try{
                                        Message msg = new Message(line);
                                        for(SocketChannel sc: clients)
                                        {
                                            sc.write(ByteBuffer.wrap(msg.getBytes()));
                                        }
                                    }catch (Exception e){
                                        leftover=line;
                                    }
                                }
                                client.write(buffer);
                            }
                            else {
                                client.close();
                            }
                        }

                    }
                    catch (SocketException e) {
                        System.err.println("Client disconnected: "+ key);
                        clients.remove(key.channel());
                        key.cancel();
                    }
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }




    }


}
