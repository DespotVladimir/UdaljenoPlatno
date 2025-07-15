package Client;

import Common.Message;
import Common.Shape;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

public class ClientGUI extends Application {

    private CreateRoomScene createRoomScene;
    ClientConnection connection;
    Thread connectionThread;

    private ListView <String> sobeLista;

    public ClientConnection getConnection() {
        return connection;
    }


    public ClientGUI(){

    }

    public void start(String[] args) throws Exception {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        initConnection();
        //canvasPage(stage);
        showStartScene(stage);
    }

    public synchronized void initConnection(){
        try {
            connection = new ClientConnection(InetAddress.getByName("localhost"),12345,this);
            connectionThread = new Thread(connection);
            connectionThread.start();
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect to the server. ");
        }
    }

    public void showStartScene(Stage stage) {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);

        Button btnCreateRoom = new Button("Kreiraj sobu");

        btnCreateRoom.setOnAction(e -> {
            if (createRoomScene == null) {
                createRoomScene = new CreateRoomScene(this, stage);
            }
            createRoomScene.prikazi();
        });

        root.getChildren().add(btnCreateRoom);

        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
        stage.setTitle("Lista soba");
        stage.show();
    }


    private Canvas canvas;
    public void canvasPage(Stage stage){
        VBox root = new VBox();
        VBox canvasBG = new VBox();
        canvas = new Canvas(800,600);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        final double[] pressedX = new double[1];
        final double[] pressedY = new double[1];
        final double[] width = {gc.getLineWidth()};

        final Color[] color = {Color.BLACK};

        final Shape[] shape = {Shape.LINE};

        canvas.setOnMousePressed(event->{
            pressedX[0] = event.getX();
            pressedY[0] = event.getY();
            MouseButton button = event.getButton();
            switch (button){
                case MouseButton.PRIMARY-> {
                    color[0] = Color.BLACK;
                    shape[0] = Shape.LINE;
                }
                case MouseButton.SECONDARY -> {
                    color[0] = Color.RED;
                    shape[0] = Shape.OVAL;
                }
                case MouseButton.MIDDLE -> {
                    shape[0] = Shape.REST;
                    try{
                        sendMessageToServer(shape[0],0,0,Math.abs(pressedX[0]-0),Math.abs(pressedY[0]-0),width[0],color[0]);
                    } catch (SocketException e) {
                        stage.close();
                        throw new RuntimeException(e);
                    }
                }
            }
            gc.setStroke(color[0]);
        });

        canvas.setOnMouseDragged(event->{
            double currentX = event.getX();
            double currentY = event.getY();

            if(shape[0]==Shape.LINE){
                gc.strokeLine(pressedX[0], pressedY[0], currentX, currentY);
                try{
                    sendMessageToServer(shape[0],pressedX[0],pressedY[0],currentX,currentY,width[0],color[0]);
                } catch (SocketException e) {
                    stage.close();
                    throw new RuntimeException(e);
                }
                pressedX[0]= currentX;
                pressedY[0] = currentY;
            }
        });

        canvas.setOnMouseReleased(event->{
            double currentX = event.getX();
            double currentY = event.getY();
            double topLeftX = Math.min(pressedX[0], currentX);
            double topLeftY = Math.min(pressedY[0], currentY);

            if(shape[0]==Shape.RECT){
                gc.strokeRect(topLeftX, topLeftY,Math.abs(pressedX[0]-currentX), Math.abs(pressedY[0]-currentY) );
                try{
                    sendMessageToServer(shape[0],topLeftX,topLeftY,Math.abs(pressedX[0]-currentX),Math.abs(pressedY[0]-currentY),width[0],color[0]);
                } catch (SocketException e) {
                    stage.close();
                    throw new RuntimeException(e);
                }
            }
            else if (shape[0]==Shape.OVAL)
            {
                gc.strokeOval(topLeftX, topLeftY,Math.abs(pressedX[0]-currentX), Math.abs(pressedY[0]-currentY));
                try{
                    sendMessageToServer(shape[0],topLeftX,topLeftY,Math.abs(pressedX[0]-currentX),Math.abs(pressedY[0]-currentY),width[0],color[0]);
                } catch (SocketException e) {
                    stage.close();
                    throw new RuntimeException(e);
                }
            }

        });


        canvasBG.getChildren().add(canvas);
        canvasBG.setMaxWidth(600);
        root.getChildren().add(canvasBG);
        Scene scene = new Scene(root,800,600);
        canvasBG.setStyle("-fx-background-color: #ffffff;");
        root.setStyle("-fx-background-color: #767575;");
        stage.setScene(scene);
        stage.setTitle("Paint");
        stage.show();


    }

    public void serverDraw(Message message) {
        double[] m = message.getCoordinates();
        Color color = message.getColor();
        Shape s = message.getShape();
        canvas.getGraphicsContext2D().setStroke(color);

        switch (s){
            case LINE:
                canvas.getGraphicsContext2D().strokeLine(m[0],m[1],m[2],m[3]);
                break;
            case RECT:
                canvas.getGraphicsContext2D().strokeRect(m[0],m[1],m[2],m[3]);
                break;
            case OVAL:
                canvas.getGraphicsContext2D().strokeOval(m[0],m[1],m[2],m[3]);
                break;
            case REST:
                canvas.getGraphicsContext2D().clearRect(0,0,canvas.getWidth(),canvas.getHeight());
                break;
        }
    }

    public void sendMessageToServer(Shape shape, double x1, double y1, double x2, double y2,double width, Color color) throws SocketException {
        try{
            Message msg = new Message(shape,x1,y1,x2,y2,width,color);
            connection.sendMessage(msg);
        } catch (SocketException e) {
            throw new SocketException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws Exception {
        connection.setClosed(true);
        super.stop();
    }
}
