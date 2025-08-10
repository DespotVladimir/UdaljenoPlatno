package Client;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;

public class ClientGUIStart extends Application {

    private ClientConnection connection;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        connectionInit();

        String FontName = "Consolas";

        VBox root = new VBox(50);
        root.setAlignment(Pos.CENTER);

        Label lblTitle = new Label("Distant canvas");
        lblTitle.setFont(Font.font(FontName,60));

        VBox vb = new VBox(5);
        HBox hDetails = new HBox(10);
        Label lblName = new Label("Enter name: ");
        lblName.setFont(Font.font(FontName,15));
        TextField txtName = new TextField();
        txtName.setFont(Font.font(FontName,15));
        Button btnConnect = new Button("Connect");
        btnConnect.setFont(Font.font(FontName,15));
        Label lblError = new Label("");
        lblError.setLabelFor(txtName);
        lblError.setFont(Font.font(FontName,FontPosture.ITALIC,15));
        lblError.setTextFill(Paint.valueOf("red"));


        hDetails.getChildren().addAll(lblName, txtName,btnConnect);
        hDetails.setAlignment(Pos.CENTER);
        vb.getChildren().addAll(hDetails,lblError);
        vb.setAlignment(Pos.CENTER);

        root.getChildren().addAll(lblTitle,vb);
        Scene scene = new Scene(root,800,600);
        stage.setScene(scene);
        stage.setTitle("Client");
        stage.show();


        final boolean[] inUse = {false};
        btnConnect.setOnAction(_ -> {
            if(inUse[0])
               return;

            String ime = txtName.getText().trim();
            if (!ime.isEmpty()) {
                Boolean response;
                inUse[0] = true;
                try{

                    lblError.setTextFill(Paint.valueOf("black"));
                    lblError.setText("Molimo pričekajte...");
                    response = connection.getServerApproval("IME|"+ime);
                } catch (IOException e) {
                    response=null;
                    System.err.println(e.getMessage());
                }
                inUse[0] = false;

                if(response==null)
                    return;

                if(!response) {
                    lblError.setTextFill(Paint.valueOf("red"));
                    lblError.setText("Ime je već u upotrebi");
                    return;
                }



                try {
                    ClientGUI clientGUI = new ClientGUI();
                    clientGUI.setUsername(ime);

                    clientGUI.setConnection(connection);
                    connection.setGui(clientGUI);

                    clientGUI.start(new Stage());
                    stage.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            else
                lblError.setText("Unesite ime");
        });


    }

    private void connectionInit() throws IOException {
        connection = new ClientConnection(InetAddress.getByName("localhost"), 12345, null);
    }
}
