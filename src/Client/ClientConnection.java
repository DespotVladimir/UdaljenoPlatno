package Client;

import Common.Message;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class ClientConnection extends Thread {


    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private ClientGUI gui;

    ClientConnection(InetAddress address, int port, ClientGUI gui) throws IOException {
        this.socket = new Socket(address, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.gui = gui;
    }

    public void sendMessage(String message) throws IOException {
        message = message.trim() + "\n";
        out.write(message);
        out.flush();
    }

    public void sendDrawMessage(Message message) throws IOException {
        String send = message.toString().trim() + "\n";
        out.write(send);
        out.flush();
    }

    private boolean closed = false;
    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    private boolean drawing = false;
    public void setDrawing(boolean drawing) {
        this.drawing = drawing;
    }

    @Override
    public void run() {
        try{
            while(!closed) {
                try{
                    String message = in.readLine();
                    if(!drawing) {
                        Message msg = new Message(message);
                        gui.serverDraw(msg);
                    }
                }catch (IOException e){
                    System.out.println(e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setGui(ClientGUI gui) {
        this.gui = gui;
    }
}
