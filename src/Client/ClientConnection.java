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

        if (gui!=null)
            gui.setConnection(this);
    }

    public void sendMessage(String message) throws IOException {
        message = message.trim() + "\n";
        out.write(message);
        out.flush();
    }

    public void sendDrawMessage(Message message) throws IOException {
        String send = new String(message.getBytes()).trim() + "\n";
        out.write(send);
        out.flush();
    }

    public boolean getServerApproval(String message) throws IOException {
        sendMessage(message);
        String response = in.readLine().trim();
        //System.out.println(message+"///"+response); //TODO obrisi
        return response.equals("POTVRDI");
    }

    public String getServerResponse(String message) throws IOException {
        sendMessage(message);
        String response = in.readLine().trim();
        //System.out.println(message+"///"+response);   //TODO obrisi
        return response;
    }

    public void clearBackLog() {
        try{
            while(in.ready())
                in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean closed = false;
    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    private boolean drawing = false;
    public void setDrawing(boolean drawing) {
        this.drawing = drawing;
    }

    public void closeResources(){
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private boolean waiting = false;
    @Override
    public void run() {

        try(BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            while(!closed) {
                try{
                    Thread.yield();     // budi thread, u suprotnom spava
                    if(drawing&&in.ready()&&!isWaiting()){
                        String message;
                        waiting=true;
                        if((message = in.readLine()) == null)
                            break;

                        if(drawing) {
                            try{
                                Message msg = new Message(message);
                                gui.serverDraw(msg);
                            }catch(Exception _){

                            }
                        }
                        waiting=false;
                    }

                }catch (IOException e){
                    System.out.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        closeResources();

    }



    public void setGui(ClientGUI gui) {
        this.gui = gui;
        gui.setConnection(this);
    }


    public boolean isDrawing() {
        return drawing;
    }

    public boolean isWaiting() {
        return waiting;
    }
}
