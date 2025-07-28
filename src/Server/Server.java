package Server;

import Common.Message;
import Common.Room;
import Common.User;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Server extends Application {

    public static int PORT = 12345;

    //Mapa soba i id, id se povecava za jedan kada je nova soba napravljena
    private static Map<String, Room> roomMap;


    //Globalna lista korisnika
    private static List<User> userList;

    private static Selector selector;

    public static void main(String[] args)  {
        Thread thread = new Thread(() -> {
            initServer();
        });
        thread.start();
        launch(args);

    }

    private static void initServer(){
        roomMap = new HashMap<>();
        userList = new ArrayList<>();

        selector = null;
        try {

            //Otvaranje servera
            selector = Selector.open();
            ServerSocketChannel server = ServerSocketChannel.open();
            InetSocketAddress address = new InetSocketAddress("localhost", PORT);
            server.bind(address);
            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.err.println("Server started on address: "+ InetAddress.getLocalHost().getHostAddress() + ", port: " + PORT);

            while (true) {
                try{
                    selector.select();

                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = keys.iterator();

                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        try{
                            if (key.isAcceptable()) {
                                acceptClient(key);
                            }
                            else if (key.isReadable()) {
                                readFromClient(key);
                            }

                        }
                        catch (SocketException e) {
                            removeClient(key);
                        }
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendMessageToUser(User user, String message) throws IOException {
        message = message.trim() + "\n";
        user.getSocket().write(ByteBuffer.wrap(message.getBytes()));
    }

    private static void acceptClient(SelectionKey key) throws IOException {
        ServerSocketChannel srv = (ServerSocketChannel) key.channel();
        SocketChannel client = srv.accept();
        client.configureBlocking(false);

        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE);

        User u = new User(null,null,client);
        userList.add(u);

        clientKey.attach(u);

        System.err.println("Client accepted. " + client.getRemoteAddress());
    }

    private static void removeClient(SelectionKey key) throws IOException {
        User u = (User) key.attachment();
        String room = u.getRoomID();

        if(roomMap.containsKey(room))
            roomMap.get(room).getUsers().remove(u);
        userList.remove(u);

        key.attach(null);
        key.channel().close();
        key.cancel();
        System.err.println("Client disconnected: "+ key);
    }

    private static void readFromClient(SelectionKey key) throws IOException {

        // Cita naredbe klijenta koje su cijele tj. sadrze '\n' na kraju pa ih obradjuje
        // Ako nisu cijele ostatak ostaje u bufferu

        User user = (User) key.attachment();

        ByteBuffer buffer = user.getBuffer();
        int bytesRead = user.getSocket().read(buffer);
        if (bytesRead == -1) {
            removeClient(key);
            return;
        }

        buffer.flip();
        while (buffer.hasRemaining()) {
            int startPos = buffer.position();
            int limit = buffer.limit();
            int newlinePos = -1;

            for (int i = startPos; i < limit; i++) {
                if (buffer.get(i) == '\n') {
                    newlinePos = i;
                    break;
                }
            }

            if (newlinePos == -1) {
                buffer.position(startPos);
                buffer.limit(limit);
                buffer.compact();
                return;
            }


            int messageLength = newlinePos - startPos + 1;
            byte[] messageBytes = new byte[messageLength];
            buffer.get(messageBytes);

            String message = new String(messageBytes, StandardCharsets.UTF_8).trim();
            System.out.println(message); // TODO REMOVE
            proccesRequest(user,message);
        }
        buffer.compact();
    }

    private static void proccesRequest(User user, String message) throws IOException {
        if(message.startsWith("IME|")){ // IME|ime
            getUserName(user,message);
        } else if (message.startsWith("NSOBA|")) { // NSOBA|naziv;lozinka
            makeNewRoom(message);
        } else if (message.startsWith("SOBE")) { // SOBE
            getListOfRooms(user);
        } else if (message.startsWith("CRTAJ|")) { // CRTAJ|oblik;x1;y1;x2;y2;R;G;B;O;W
            drawUser(user,message);
        } else if (message.startsWith("ULAZ|")) { // ULAZ|id;lozinka
            checkRoomPassword(user, message);
        } else if (message.startsWith("IZLAZ")) { // IZLAZ
            exitUserFromRoom(user);
        } else if (message.startsWith("KRAJ")) { // KRAJ
            exitUserFromRoom(user);
        } else if (message.startsWith("INFO|")) { // INFO|id
            getRoomInfo(user,message);
        } else if (message.startsWith("PLATNO")) { // PLATNO|id
           getCanvasImage(user,message);
        } else{
            sendMessageToUser(user,"ERROR");
        }
    }

    private static void drawUser(User user, String msg) throws IOException {
        int start = "CRTAJ|".length();
        String s = msg.substring(start);
        String roomID = user.getRoomID();

        if(roomID != null){
            if(roomMap.containsKey(roomID))
            {
                Message message = new Message(s);
                Room room = roomMap.get(roomID);
                room.changeStrokeColor(message.getColor());
                room.changeLineWidth(message.getWidth());
                double x1=0,y1=0,x2=0,y2=0;
                switch (message.getShape()){
                    case LINE -> {
                        x1=message.getCoordinates()[0];
                        y1=message.getCoordinates()[1];
                        x2=message.getCoordinates()[2];
                        y2=message.getCoordinates()[3];
                        room.drawLineOnCanvas(x1,y1,x2,y2);
                    }
                    case OVAL -> {
                        x1=Math.min(message.getCoordinates()[0],message.getCoordinates()[2]);
                        y1=Math.min(message.getCoordinates()[1],message.getCoordinates()[3]);
                        x2=Math.abs(message.getCoordinates()[0]-message.getCoordinates()[2]);
                        y2=Math.abs(message.getCoordinates()[1]-message.getCoordinates()[3]);
                        room.drawOvalOnCanvas(x1,y1,x2,y2);
                    }
                    case RECT -> {
                        x1=Math.min(message.getCoordinates()[0],message.getCoordinates()[2]);
                        y1=Math.min(message.getCoordinates()[1],message.getCoordinates()[3]);
                        x2=Math.abs(message.getCoordinates()[0]-message.getCoordinates()[2]);
                        y2=Math.abs(message.getCoordinates()[1]-message.getCoordinates()[3]);
                        room.drawRectOnCanvas(x1,y1,x2,y2);
                    }
                    case REST -> {
                        room.resetCanvas();
                    }
                    case null, default -> {
                        sendMessageToUser(user,"ERROR");
                        return;
                    }
                }
                message.setCoordinates(x1,y1,x2,y2);
                sendMessageToUser(user,message.getFormatedMessage());

            }else
                sendMessageToUser(user,"ERROR");
        }else
            sendMessageToUser(user,"ERROR");
    }

    private static void getCanvasImage(User user, String s) throws IOException {
        int start = "PLATNO|".length();
        s = s.substring(start);

        if(roomMap.containsKey(s)){
            String canvasBASE64 = roomMap.get(s).exportCanvasToBase64();
            sendMessageToUser(user,canvasBASE64);
        }
        else
            sendMessageToUser(user,"ERROR");
    }


    private static void getUserName(User user,String s) throws IOException {
        int start = "IME|".length();
        s = s.substring(start);
        if(isNameAvailable(s))
        {
            user.setName(s);
            sendMessageToUser(user,"POTVRDI");
        }
        else
            sendMessageToUser(user,"ERROR");
    }

    private static boolean isNameAvailable(String name){
        for(User u : userList)
        {
            if(u.getName()==null)
                continue;

            if(u.getName().equals(name))
                return false;
        }
        return true;
    }

    private static void makeNewRoom(String s) {
        int start = "NSOBA|".length();
        String roomName = s.substring(start).split(";")[0];
        String roomPswd="";
        if(s.split(";").length>1)
            roomPswd=s.split(";")[1];

        String RoomID = UUID.randomUUID().toString().substring(0, 8);
        Room r = new Room(roomPswd,roomName,RoomID);
        roomMap.put(RoomID,r);

    }


    private static void getListOfRooms(User user) throws IOException {

        // Vraca listu oblika ime:kljuc_sobe odvojena ';'

        if(roomMap.isEmpty())
            return;

        StringBuilder rooms = new StringBuilder();
        for(String key : roomMap.keySet()){
            rooms.append(roomMap.get(key).getRoomName()).append(":").append(key).append(";");
        }
        rooms.deleteCharAt(rooms.length()-1);
        sendMessageToUser(user,rooms.toString());
    }

    private static void checkRoomPassword(User user, String message) throws IOException {
        int start = "ULAZ|".length();
        message = message.substring(start).trim();
        String id = message.split(";")[0];
        String pswd = "";

        if(message.split(";").length>1)
            pswd = message.split(";")[1];

        if(roomMap.containsKey(id))
        {
            if(roomMap.get(id).getRoomPassword().equals(pswd))
            {
                user.setRoomID(id);
                sendMessageToUser(user,"POTVRDI");
            }
            else
                sendMessageToUser(user,"ERROR");
        }
        else
            sendMessageToUser(user,"ERROR");
    }

    private static void getRoomInfo(User user, String message) throws IOException {

        // Vraca listu korisnika sobe odvojene ';'

        int start = "INFO|".length();
        String id = message.substring(start).trim();
        if(!roomMap.containsKey(id)){
            sendMessageToUser(user,"ERROR");
            return;
        }
        StringBuilder users = new StringBuilder();
        for(User u : roomMap.get(id).getUsers()){
            users.append(u.getName()).append(";");
        }
        sendMessageToUser(user,users.toString());
    }

    private static void checkRoomStatus(String roomID){

        // Brise sobu ako nema niko u njoj kada neko izadje

        if(roomMap.containsKey(roomID))
            if(roomMap.get(roomID).getUsers().isEmpty())
                roomMap.remove(roomID);
    }

    private static void exitUserFromRoom(User user) {
        if(roomMap.containsKey(user.getRoomID())){
            roomMap.remove(user.getRoomID());
            checkRoomStatus(user.getRoomID());
            user.setRoomID(null);
        }
    }


    @Override
    public void start(Stage stage) throws Exception {

    }
}
