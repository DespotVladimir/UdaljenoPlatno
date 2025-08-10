package Client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

public class CreateRoomScene {
    private ClientGUI gui;
    private Stage stage;



    public CreateRoomScene(ClientGUI gui, Stage stage){
        this.gui=gui;
        this.stage=stage;
    }

    public void prikazi(){
        String fontName="Consolas";

        VBox root=new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label naslov=new Label("Napravi novu sobu");
        naslov.setFont(Font.font(fontName,30));

        TextField tfNazivSobe=new TextField();
        tfNazivSobe.setPromptText("Naziv sobe");

        PasswordField pfPassword=new PasswordField();
        pfPassword.setPromptText("Lozinka sobe");


        HBox dugmici=new HBox(10);
        dugmici.setAlignment(Pos.CENTER);
        Button btnKreiraj=new Button("Kreiraj sobu");
        Button btnNazad=new Button("Nazad");

        dugmici.getChildren().addAll(btnKreiraj,btnNazad);
        root.getChildren().addAll(naslov,tfNazivSobe,pfPassword,dugmici);

        Scene scene=new Scene(root,400,300);
        stage.setScene(scene);
        stage.setTitle("Kreiraj sobe");
        stage.show();

        btnNazad.setOnAction(_ -> gui.ListaSobaScene(stage));



        btnKreiraj.setOnAction(_ -> {
            String naziv=tfNazivSobe.getText().trim();
            String lozinka=pfPassword.getText().trim();

            if(naziv.isEmpty()){
                pokazatiAlert("Naziv sobe ne moze biti prazan!");
                return;
            }

            String poruka;
            if(lozinka.isEmpty()){
                poruka = "NSOBA|" + naziv + ";";
            }else {
                poruka = "NSOBA|" + naziv + ";" + lozinka;
            }

            try {
                gui.getConnection().sendMessage(poruka);
                gui.ListaSobaScene(stage);
            } catch (Exception e) {
                pokazatiAlert("Greška prilikom slanja poruke serveru.");
            }


        });



    }

    private void pokazatiAlert(String poruka) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Greška");
        alert.setHeaderText(null);
        alert.setContentText(poruka);
        alert.showAndWait();
    }

}
