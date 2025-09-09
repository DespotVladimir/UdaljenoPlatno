package Server;

import Common.Message;
import Common.Room;
import Common.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class Server extends Application {

    public static int PORT = 12345;

    //Mapa soba i id, id se povecava za jedan kada je nova soba napravljena
    private static Map<String, Room> roomMap;


    //Globalna lista korisnika
    private static List<User> userList;

    private static Selector selector;

    public static void main(String[] args)  {
        Thread thread = new Thread(() -> initServer());
        thread.start();
        launch(args);

    }

    private static void initServer(){
        roomMap = new HashMap<>();
        Room r = new Room("","Glavna soba","00000000");
        roomMap.put("00000000",r);

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
        System.err.println("Client disconnected: "+ key.channel().toString());
        key.channel().close();
        key.cancel();
    }

    private static void readFromClient(SelectionKey key) throws IOException, InterruptedException {

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
            //System.out.println(message); // TODO REMOVE
            processRequest(user,message);
        }
        buffer.compact();
    }

    private static void processRequest(User user, String message) throws IOException, InterruptedException {
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
                    case REST -> room.resetCanvas();
                    case null, default -> {
                        sendMessageToUser(user,"ERROR1");
                        return;
                    }
                }
                message.setCoordinates(x1,y1,x2,y2);

                for(User u : room.getUsers()){
                    sendMessageToUser(u,message.getFormatedMessage());
                }

            }else
                sendMessageToUser(user,"ERROR2");
        }else
            sendMessageToUser(user,"ERROR3");
    }

    private static void getCanvasImage(User user, String s) throws IOException, InterruptedException {
        int start = "PLATNO|".length();
        s = s.substring(start);

        if(roomMap.containsKey(s)){
            String canvasBASE64 = roomMap.get(s).exportCanvasToBase64();
            sendMessageToUser(user,canvasBASE64);
        }
        else
            sendMessageToUser(user,"ERROR4");
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
            sendMessageToUser(user,"ERROR5");
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

        if (roomMap.isEmpty())
        {
            sendMessageToUser(user,"");
            return;
        }

        StringBuilder rooms = new StringBuilder();

        for (String key : roomMap.keySet()) {
            rooms.append(roomMap.get(key).getRoomName()).append(":").append(key).append(";");
        }

        if (rooms.charAt(rooms.length() - 1) == ';')
            rooms.deleteCharAt(rooms.length() - 1);

        sendMessageToUser(user, rooms.toString());
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
            if(roomMap.get(id).getRoomPassword().equals(pswd) || roomMap.get(id).getRoomPassword().isEmpty())
            {
                user.setRoomID(id);
                roomMap.get(id).getUsers().add(user);
                sendMessageToUser(user,"POTVRDI");
            }
            else
                sendMessageToUser(user,"ERROR6");
        }
        else
            sendMessageToUser(user,"ERROR7");
    }

    private static void getRoomInfo(User user, String message) throws IOException {

        // Vraca listu korisnika sobe odvojene ';' ime;O-otvorena ili Z-zakljucana;

        int start = "INFO|".length();
        String id = message.substring(start).trim();
        if(!roomMap.containsKey(id)){
            sendMessageToUser(user,"ERROR8");
            return;
        }
        String startR = roomMap.get(id).getRoomName()+";"+(roomMap.get(id).getRoomPassword().isEmpty()?"O;":"Z;");
        StringBuilder users = new StringBuilder(startR);
        for(User u : roomMap.get(id).getUsers()){
            users.append(u.getName()).append(";");
        }
        sendMessageToUser(user,users.toString());
    }

    private static void checkRoomStatus(String roomID){

        // Brise sobu ako nema niko u njoj kada neko izadje

        if(roomID.equals("00000000")) // Glavna soba koja se ne brise
            return;
        if(roomMap.containsKey(roomID))
            if(roomMap.get(roomID).getUsers().isEmpty())
                roomMap.remove(roomID);
    }

    private static void exitUserFromRoom(User user) {
        if(roomMap.containsKey(user.getRoomID())){
            roomMap.get(user.getRoomID()).getUsers().remove(user);
            checkRoomStatus(user.getRoomID());
            user.setRoomID(null);
        }
    }


    /***************** GUI ************************/

    private final boolean GUI = true;
    @Override
    public void start(Stage stage) throws Exception {
        if(!GUI)
            return;
        stage.setOnCloseRequest(_-> System.exit(0));

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));


        HBox tables = new HBox(50);

        VBox leftSide = new VBox(1);

        Label lblUser = new Label("RoomID:   Username");
        lblUser.setFont(new Font("Consolas",13));
        TextArea tblUsers = new TextArea();
        tblUsers.setPrefHeight(400);
        tblUsers.setPrefWidth(200);
        tblUsers.setEditable(false);
        tblUsers.setFont(new Font("Consolas",12));

        tblUsers.setBackground(Background.EMPTY);


        leftSide.getChildren().addAll(lblUser,tblUsers);


        VBox r = new VBox(1);
        Label lblR = new Label("Sobe: ");
        lblR.setFont(new Font("Consolas",13));
        FlowPane rightSide = new FlowPane();
        rightSide.setMaxHeight(400);
        rightSide.setPrefWidth(200);

        r.getChildren().addAll(lblR,rightSide);

        Button off = new Button("Ugasi Server");
        off.setFont(new Font("Consolas",13));
        off.setOnAction(_->{
            System.exit(0);
        });

        tables.getChildren().addAll(leftSide,r);

        root.getChildren().addAll(tables,off);

        Scene main = new Scene(root);
        stage.setScene(main);
        stage.setTitle("Admin Panel");
        stage.show();


        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(300), _ -> {
            refreshUserList(tblUsers,"0");
            refreshServerList(rightSide);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void refreshUserList(TextArea  userTable,String id){
        userTable.setEditable(true);
        IndexRange i = userTable.getSelection();
        userTable.setText("");
        List<User> userList1 = new ArrayList<>(userList);
        for(User u : userList1){
            if(u.getRoomID()==null)
                userTable.appendText(("--------")+": "+ (u.getName()==null?"Unknown":u.getName()) +"\n");
            else if(u.getRoomID().equals(id) || id.equalsIgnoreCase("0"))
                userTable.appendText(u.getRoomID() +": "+ u.getName() +"\n");
        }

        userTable.selectRange(i.getStart(),i.getEnd());
        userTable.setEditable(false);

    }

    private void refreshServerList(FlowPane serverTable){
        serverTable.getChildren().clear();
        for(String key:roomMap.keySet())
        {
            Button btn = new Button(roomMap.get(key).getRoomID()+": "+roomMap.get(key).getRoomName());
            btn.setFont(new Font("Consolas",12));
            btn.setPrefWidth(200);
            btn.setMaxWidth(200);
            btn.setOnAction(_->{
                HBox hAll = new HBox(10);
                hAll.setPadding(new Insets(20));

                VBox l = new VBox(10);
                Label lblName = new Label("Ime: " + roomMap.get(key).getRoomName());
                lblName.setFont(new Font("Consolas",13));
                Label lblPSW = new Label("Šifra: " + (roomMap.get(key).getRoomPassword().isEmpty()?"-":roomMap.get(key).getRoomPassword()));
                lblPSW.setFont(new Font("Consolas",13));
                Label lblID = new Label("RoomID: " + roomMap.get(key).getRoomID());
                lblID.setFont(new Font("Consolas",13));
                Label lblCount = new Label("Broj Korisnika: " + roomMap.get(key).getUsers().size());
                lblCount.setFont(new Font("Consolas",13));
                Label users = new Label("Korisnici: ");
                users.setFont(new Font("Consolas",13));
                TextArea txtUsers = new TextArea();
                txtUsers.setFont(new Font("Consolas",12));
                txtUsers.setPrefHeight(400);
                txtUsers.setPrefWidth(200);
                txtUsers.setEditable(false);

                l.getChildren().addAll(lblName,lblPSW,lblID,lblCount,users,txtUsers);

                Canvas c = roomMap.get(key).getCanvas();

                hAll.getChildren().addAll(l,c);

                Stage room = new Stage();

                Scene  scene = new Scene(hAll);
                room.setScene(scene);
                room.setTitle("RoomView");
                room.show();


                Timeline timeline = new Timeline(new KeyFrame(Duration.millis(300), _ -> {
                    lblCount.setText("Broj Korisnika: " + roomMap.get(key).getUsers().size());
                    refreshUserList(txtUsers,key);
                }));
                timeline.setCycleCount(Timeline.INDEFINITE);
                timeline.play();
            });

            serverTable.getChildren().add(btn);
        }
    }
}
