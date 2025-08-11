package Client;

import Common.Message;
import Common.Room;
import Common.Shape;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
        HBox root = new HBox(1);

        Button btnCreateRoom = new Button("Kreiraj sobu");

        btnCreateRoom.setOnAction(_ -> {
            if (createRoomScene == null) {
                createRoomScene = new CreateRoomScene(this, stage);
            }
            createRoomScene.prikazi();
        });

        sobeLista = new ListView<>();
        sobeLista.setStyle("-fx-control-inner-background: lightgray;-fx-background-color: lightgray;" +
                "-fx-font-family: 'Consolas'; -fx-font-size: 14;-fx-text-alignment: center;");
        sobeLista.setPrefHeight(150);
        sobeLista.setPrefWidth(200);

        Button btnUlazSoba = new Button("Uđi u sobu");

        VBox rightSide = new VBox(10);

        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(10);
        controls.getChildren().addAll(btnCreateRoom, btnUlazSoba);

        final VBox[] info = {new VBox(10)};
        final TextArea[] txt = {new TextArea()};


        sobeLista.setOnMousePressed(_->{
            Room selektovanaSoba = sobeLista.getSelectionModel().getSelectedItem();

            if (selektovanaSoba != null) {
                info[0].getChildren().clear();
                txt[0] = infoPanel(sobeLista.getSelectionModel().getSelectedItem().getRoomID(),info[0]);
                refreshList(txt[0],sobeLista.getSelectionModel().getSelectedItem().getRoomID());
            }
        });
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
                        System.err.println(ex.getMessage());
                    }
                });
            }
        });



        rightSide.getChildren().addAll(controls,info[0]);
        rightSide.setPadding(new Insets(15));
        rightSide.setStyle("-fx-background-color: #dddddd;");
        root.getChildren().addAll(sobeLista,rightSide);

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

        stage.setOnCloseRequest(_-> System.exit(0));
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1500), _ -> {
            try {
                osveziListuSoba();
            } catch (Exception _) {

            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        scene.windowProperty().addListener((obs, oldWindow, newWindow) -> {
            if (newWindow == null) {
                timeline.stop();
            }
        });
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
        int index = sobeLista.getSelectionModel().getSelectedIndex();
        sobeLista.getItems().clear();
        if(!sobe.isEmpty())
            sobeLista.getItems().setAll(sobe);
        sobeLista.getSelectionModel().select(index);
    }


    private Canvas canvas;
    private String roomID;
    public void canvasPage(Stage stage){
        stage.setOnCloseRequest(_-> System.exit(0));

        VBox root = new VBox();

        HBox lowerPart = new HBox();
        VBox canvasBG = new VBox();
        canvas = new Canvas(800,600);


        GraphicsContext gc = canvas.getGraphicsContext2D();

        final double[] pressedX = new double[1];
        final double[] pressedY = new double[1];


        ComboBox<Shape> shapeBox=new ComboBox<>();
        ColorPicker colorPicker=new ColorPicker(Color.BLACK);
        Slider slider = new Slider(1,50,1);
        slider.setBlockIncrement(1);
        HBox toolbar=createToolbar(stage,shapeBox,colorPicker,slider);

        final Shape[] shape={shapeBox.getValue()!=null ? shapeBox.getValue():Shape.LINE};
        final  Color[] color = {colorPicker.getValue()};

        shapeBox.valueProperty().addListener((obs,oldVal,newVal)->shape[0]=newVal);
        colorPicker.valueProperty().addListener((obs,oldVal,newVal)->color[0]=newVal);


        canvas.setOnMousePressed(event -> {
            pressedX[0] = event.getX();
            pressedY[0] = event.getY();
            gc.setStroke(color[0]);
        });

        canvas.setOnMouseDragged(event->{
            double currentX = event.getX();
            double currentY = event.getY();


            if(shape[0]==Shape.LINE){
                gc.setLineWidth(slider.getValue());
                gc.setLineCap(StrokeLineCap.ROUND);
                gc.strokeLine(pressedX[0], pressedY[0], currentX, currentY);
                try{
                    sendMessageToServer(shape[0],pressedX[0],pressedY[0],currentX,currentY,slider.getValue(),color[0]);
                } catch (SocketException e) {
                    stage.close();
                    throw new RuntimeException(e);
                }
                pressedX[0]= currentX;
                pressedY[0] = currentY;
                gc.setLineCap(StrokeLineCap.SQUARE);
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
                    sendMessageToServer(shape[0],currentX,currentY,pressedX[0],pressedY[0],slider.getValue(),color[0]);
                } catch (SocketException e) {
                    stage.close();
                    throw new RuntimeException(e);
                }
            }
            else if (shape[0]==Shape.OVAL)
            {
                gc.strokeOval(topLeftX, topLeftY,Math.abs(pressedX[0]-currentX), Math.abs(pressedY[0]-currentY));
                try{
                    sendMessageToServer(shape[0],currentX,currentY,pressedX[0],pressedY[0],slider.getValue(),color[0]);
                } catch (SocketException e) {
                    stage.close();
                    throw new RuntimeException(e);
                }
            }

        });


        canvasBG.getChildren().add(canvas);
        canvasBG.setMaxWidth(600);

        //VBox v = new VBox(10);
        //TextArea txt = infoPanel(roomID,v);



        //lowerPart.getChildren().addAll(canvasBG,v);
        lowerPart.getChildren().addAll(canvasBG);


        root.getChildren().addAll(toolbar,lowerPart);
        Scene scene = new Scene(root,800,600);
        canvasBG.setStyle("-fx-background-color: #ffffff;");
        root.setStyle("-fx-background-color: #767575;");
        stage.setScene(scene);
        stage.setTitle("Paint");
        stage.show();


        new Thread(()-> Platform.runLater(()->{
            try{
                String platno = connection.getServerResponse("PLATNO|"+roomID);
                drawBase64ToCanvas(canvas,platno);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        })).start();

        /*
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1000), _ -> {
            refreshList(txt,roomID);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();*/
    }

    private HBox createToolbar(Stage stage, ComboBox<Shape> shapeBox, ColorPicker colorPicker, Slider slider) {
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #dddddd;");

        Button btnNazad = new Button("Nazad");
        btnNazad.setFont(new Font("consolas",13));
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



        Button btnReset = new Button("Reset");
        btnReset.setFont(new Font("consolas",13));
        btnReset.setOnAction(_ -> {
            canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            try {
                sendMessageToServer(Shape.REST,0,0,0,0,0,colorPicker.getValue());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


        colorPicker.setStyle("-fx-font-size: 13; -fx-font-family: 'Consolas'");

        Label lblSlider = new Label("Širina: ");
        lblSlider.setFont(new Font("consolas",13));

        Label lblOblik = new Label("Oblik:");
        lblOblik.setFont(new Font("consolas",13));
        shapeBox.getItems().addAll(Shape.LINE, Shape.RECT, Shape.OVAL);
        shapeBox.setStyle("-fx-font-size: 13; -fx-font-family: 'Consolas';");
        shapeBox.setValue(Shape.LINE);

        Label lblBoja = new Label("Boja:");
        lblBoja.setFont(new Font("consolas",13));

        toolbar.getChildren().addAll(btnNazad, lblOblik, shapeBox, lblBoja, colorPicker,lblSlider,slider,btnReset);

        return toolbar;
    }

    private TextArea infoPanel(String roomID,VBox infoPanel){

        infoPanel.setPadding(new Insets(20));
        infoPanel.setPrefWidth(200);
        infoPanel.setBorder(new Border(new BorderStroke(Color.LIGHTGREY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        infoPanel.setStyle("-fx-background-color: #dddddd;");
        String response;
        try {
            response = connection.getServerResponse("INFO|"+roomID);
        } catch (IOException e) {
            response = "null;";
        }

        String[] parts = response.split(";");
        String name = parts[0];
        Label lblName = new Label("Ime sobe: "+name);
        lblName.setFont(new Font("Consolas",13));
        Label lblID = new Label("ID: " + roomID);
        lblID.setFont(new Font("Consolas",13));
        Label lblStatus = new Label("Status: " + ((parts[1].equals("O")?"Otvoreno":"Zakljucano")));
        lblStatus.setFont(new Font("Consolas",13));
        Label lblUser = new Label("Korisnici: ");
        lblUser.setFont(new Font("Consolas",13));
        TextArea txtInfo = new TextArea();
        txtInfo.setFont(new Font("Consolas",12));
        txtInfo.setEditable(false);
        txtInfo.setWrapText(true);
        txtInfo.setMaxWidth(160);
        txtInfo.setPrefWidth(160);
        txtInfo.setMaxHeight(200);
        txtInfo.setStyle("-fx-control-inner-background: #d3d3d3;");

        if (parts.length>2){
            for(int i=2;i<parts.length;i++){
                txtInfo.appendText(i-1+":"+parts[i]+"\n");
            }
        }
        infoPanel.getChildren().addAll(lblName, lblID,lblStatus,lblUser,txtInfo);
        return txtInfo;

    }

    private void refreshList(TextArea txtInfo,String roomID){
        String response;
        boolean d = connection.isDrawing();

        try {
            if(connection.isWaiting())
                return;

            response = connection.getServerResponse("INFO|"+roomID);

            if(response.startsWith("LINE")||response.contains("REST")||response.contains("OVAL")||response.contains("RECT")) {
                System.out.println("INFO: "+response);
                Message msg = new Message(response);
                serverDraw(msg);
                return;
            }

        } catch (IOException e) {
            response = "null;";
        }
        String[] parts = response.split(";");
        IndexRange i = txtInfo.getSelection();
        txtInfo.clear();
        if (parts.length>2){
            for(int j=2;j<parts.length;j++){
                txtInfo.appendText(j-1+":"+parts[j]+"\n");
            }
        }
        txtInfo.selectRange(i.getStart(),i.getEnd());
        connection.setDrawing(d);
    }

    public void serverDraw(Message message) {
        try{
            double[] m = message.getCoordinates();
            Color color = message.getColor();
            Shape s = message.getShape();
            canvas.getGraphicsContext2D().setStroke(color);
            canvas.getGraphicsContext2D().setLineWidth(message.getWidth());
            switch (s){
                case LINE:
                    canvas.getGraphicsContext2D().setLineCap(StrokeLineCap.ROUND);
                    canvas.getGraphicsContext2D().strokeLine(m[0],m[1],m[2],m[3]);
                    canvas.getGraphicsContext2D().setLineCap(StrokeLineCap.SQUARE);
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
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void sendMessageToServer(Shape shape, double x1, double y1, double x2, double y2,double width, Color color) throws SocketException {
        try{
            Message msg = new Message(shape,x1,y1,x2,y2,width,color);
            connection.sendDrawMessage(msg);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void drawBase64ToCanvas(Canvas canvas, String base64) {
        try {

            byte[] imageBytes = Base64.getDecoder().decode(base64);
            Image image = new Image(new ByteArrayInputStream(imageBytes));

            // Crtanje na Canvas
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.drawImage(image, 0, 0);
            connection.setDrawing(true);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }



}
