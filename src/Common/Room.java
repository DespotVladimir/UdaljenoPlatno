package Common;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Room {
    private String roomPassword;
    private String roomName;
    private Canvas canvas;
    private String roomID;
    private List<User> users;

    public Room(String roomPassword, String roomName, String roomID) {
        this.roomPassword = roomPassword;
        this.roomName = roomName;
        this.roomID = roomID;
        canvas = new Canvas(800,600);
        users = new ArrayList<>();
    }

    public Room(String roomName, String roomID){
        this.roomName = roomName;
        this.roomID = roomID;
        canvas = null;
        users = null;
    }

    public void drawLineOnCanvas(double x1, double y1, double x2, double y2) {
        Platform.runLater(()->{
            canvas.getGraphicsContext2D().strokeLine(x1, y1, x2, y2);
        });
    }

    public void drawRectOnCanvas(double x, double y, double w, double h) {
        Platform.runLater(()->{
            canvas.getGraphicsContext2D().strokeRect(x, y, w, h);
        });
    }

    public void drawOvalOnCanvas(double x, double y, double w, double h) {
        Platform.runLater(()->{
            canvas.getGraphicsContext2D().strokeOval(x, y, w, h);
        });
    }

    public void resetCanvas(){
        Platform.runLater(()->{
            canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        });

    }

    public void changeStrokeColor(Paint color){
        Platform.runLater(()->{
            canvas.getGraphicsContext2D().setFill(color);
        });

    }

    public void changeLineWidth(double width){
        Platform.runLater(()->{
            canvas.getGraphicsContext2D().setLineWidth(width);
        });

    }

    public String exportCanvasToBase64() throws IOException {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        WritableImage writableImage = new WritableImage(width, height);
        Platform.runLater(()->{
            canvas.snapshot(null, writableImage);
        });

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixelReader = writableImage.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color fxColor = pixelReader.getColor(x, y);
                int argb =
                        ((int)(fxColor.getOpacity() * 255) << 24) |
                                ((int)(fxColor.getRed() * 255) << 16) |
                                ((int)(fxColor.getGreen() * 255) << 8) |
                                ((int)(fxColor.getBlue() * 255));
                bufferedImage.setRGB(x, y, argb);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public String getRoomPassword() {
        return roomPassword;
    }

    public String getRoomName() {
        return roomName;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public String getRoomID() {
        return roomID;
    }

    public List<User> getUsers() {
        return users;
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomName='" + roomName + '\'' +
                ", roomPassword='" + roomPassword + '\'' +
                ", canvas=" + canvas +
                ", roomID='" + roomID + '\'' +
                '}';
    }
}
