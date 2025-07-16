package Client;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.InetAddress;

public class ClientGUIStart extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        String FontName = "Consolas";

        VBox root = new VBox(50);
        root.setAlignment(Pos.CENTER);

        Label lblTitle = new Label("Distant canvas");
        lblTitle.setFont(Font.font(FontName,60));

        HBox hDetails = new HBox(10);
        Label lblName = new Label("Enter name: ");
        lblName.setFont(Font.font(FontName,15));
        TextField txtName = new TextField();
        txtName.setFont(Font.font(FontName,15));
        Button btnConnect = new Button("Connect");
        btnConnect.setFont(Font.font(FontName,15));

        hDetails.getChildren().addAll(lblName, txtName,btnConnect);
        hDetails.setAlignment(Pos.CENTER);

        root.getChildren().addAll(lblTitle,hDetails);
        Scene scene = new Scene(root,800,600);
        stage.setScene(scene);
        stage.setTitle("Client");
        stage.show();

        btnConnect.setOnAction(e -> {
            String ime = txtName.getText().trim();
            if (!ime.isEmpty()) {
                try {
                    ClientGUI clientGUI = new ClientGUI();
                    clientGUI.setUsername(ime);
                    ClientConnection connection = new ClientConnection(InetAddress.getByName("localhost"), 12345, clientGUI);
                    clientGUI.setConnection(connection);
                    connection.setGui(clientGUI);
                    new Thread(connection).start();
                    clientGUI.start(new Stage());
                    stage.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });


    }
}
