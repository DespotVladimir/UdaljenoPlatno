package Client;

import Common.Message;
import Common.Room;
import Common.Shape;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class ClientGUI extends Application {

    private String korisnickoIme;


    private CreateRoomScene createRoomScene;
    ClientConnection connection;
    Thread connectionThread;

    private ListView <Room> sobeLista;


    public ClientConnection getConnection() {
        return connection;
    }

    public void setUsername(String korisnickoIme) {
        this.korisnickoIme = korisnickoIme;
    }

    public void setConnection(ClientConnection connection) {
        this.connection = connection;
    }


    public ClientGUI(){

    }

    public void start(String[] args) throws Exception {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        connectionThread = new Thread(connection);
        connectionThread.start();
        ListaSobaScene(stage);
    }


    public void ListaSobaScene(Stage stage) {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);

        Button btnCreateRoom = new Button("Kreiraj sobu");

        btnCreateRoom.setOnAction(_ -> {
            if (createRoomScene == null) {
                createRoomScene = new CreateRoomScene(this, stage);
            }
            createRoomScene.prikazi();
        });

        sobeLista = new ListView<>();
        sobeLista.setPrefHeight(150);
        sobeLista.setPrefWidth(200);

        Button btnUlazSoba=new Button("Uđi u sobu");

        btnUlazSoba.setOnAction(_ -> {
            Room selektovanaSoba = sobeLista.getSelectionModel().getSelectedItem();
            if (selektovanaSoba != null) {

                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Ulazak u sobu");
                dialog.setHeaderText("Soba \"" + selektovanaSoba.getRoomName() + "\" može biti zaštićena.");
                dialog.setContentText("Unesite šifru (ostavite prazno ako nema):");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(password -> {
                    try {
                        if(connection.getServerApproval("ULAZ|" + selektovanaSoba.getRoomID() + ";" + password))
                        {
                            roomID = selektovanaSoba.getRoomID();
                            canvasPage(stage);
                        }
                        else
                            dialog.close();

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
            }
        });


        root.getChildren().addAll(sobeLista,btnCreateRoom,btnUlazSoba);

        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
        stage.setTitle("Lista soba");
        stage.show();


        new Thread(()-> Platform.runLater(()->{

            try {
                osveziListuSoba();
            } catch (IOException e){
                stage.close();
            }
        })).start();

    }






    public void osveziListuSoba() throws IOException {
        List<Room> sobe = new ArrayList<>();
        try {
            String odgovor = connection.getServerResponse("SOBE");
            String[] s = odgovor.split(";");
            for(String str: s){
                String[] parts = str.split(":");
                Room r = new Room(parts[0],parts[1]);
                sobe.add(r);
            }
        } catch (IOException e) {
            throw new IOException(e);
        }

        sobeLista.getItems().clear();
        if(!sobe.isEmpty())
            sobeLista.getItems().setAll(sobe);

    }


    private Canvas canvas;
    private String roomID;
    public void canvasPage(Stage stage){

        VBox root = new VBox();
        VBox canvasBG = new VBox();
        canvas = new Canvas(800,600);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        final double[] pressedX = new double[1];
        final double[] pressedY = new double[1];
        final double[] width = {gc.getLineWidth()};

        ComboBox<Shape> shapeBox=new ComboBox<>();
        ColorPicker cololrPicker=new ColorPicker(Color.BLACK);
        HBox toolbar=createToolbar(stage,shapeBox,cololrPicker);

        final Shape[] shape={shapeBox.getValue()!=null ? shapeBox.getValue():Shape.LINE};
        final  Color[] color = {cololrPicker.getValue()};

        shapeBox.valueProperty().addListener((obs,oldVal,newVal)->shape[0]=newVal);
        cololrPicker.valueProperty().addListener((obs,oldVal,newVal)->color[0]=newVal);


        /*canvas.setOnMousePressed(event->{
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
        });*/

        canvas.setOnMousePressed(event -> {
            pressedX[0] = event.getX();
            pressedY[0] = event.getY();
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


        root.getChildren().addAll(toolbar,canvasBG);
        Scene scene = new Scene(root,800,600);
        canvasBG.setStyle("-fx-background-color: #ffffff;");
        root.setStyle("-fx-background-color: #767575;");
        stage.setScene(scene);
        stage.setTitle("Paint");
        stage.show();


        new Thread(()-> Platform.runLater(()->{
            //TODO poruka->platno
            try{
                String platno = connection.getServerResponse("PLATNO|"+roomID);
                drawBase64ToCanvas(canvas,platno);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        })).start();
    }

    private HBox createToolbar(Stage stage, ComboBox<Shape> shapeBox, ColorPicker colorPicker) {
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #dddddd;");

        Button btnNazad = new Button("Nazad");
        btnNazad.setOnAction(_ -> {
            connection.setDrawing(false);
            connection.clearBackLog();
            try {
                connection.sendMessage("IZLAZ");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ListaSobaScene(stage);
        });

        Label lblOblik = new Label("Oblik:");
        shapeBox.getItems().addAll(Shape.LINE, Shape.RECT, Shape.OVAL, Shape.REST);
        shapeBox.setValue(Shape.LINE);

        Label lblBoja = new Label("Boja:");

        toolbar.getChildren().addAll(btnNazad, lblOblik, shapeBox, lblBoja, colorPicker);

        return toolbar;
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
            connection.sendDrawMessage(msg);
        } catch (SocketException e) {
            throw new SocketException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void drawBase64ToCanvas(Canvas canvas, String base64) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64);

            InputStream is = new ByteArrayInputStream(imageBytes);
            BufferedImage bufferedImage = ImageIO.read(is);

            if (bufferedImage == null) {
                System.err.println("Greška: nije moguće učitati sliku iz Base64.");
                return;
            }

            // Pretvori BufferedImage u JavaFX Image
            WritableImage fxImage = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
            PixelWriter pw = fxImage.getPixelWriter();

            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    int argb = bufferedImage.getRGB(x, y);
                    int alpha = (argb >> 24) & 0xFF;
                    int red = (argb >> 16) & 0xFF;
                    int green = (argb >> 8) & 0xFF;
                    int blue = argb & 0xFF;

                    Color fxColor = Color.rgb(
                            red, green, blue, alpha / 255.0
                    );

                    pw.setColor(x, y, fxColor);
                }
            }


            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.drawImage(fxImage, 0, 0);
            connection.setDrawing(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void stop() throws Exception {
        connection.setClosed(true);
        super.stop();
    }
}
