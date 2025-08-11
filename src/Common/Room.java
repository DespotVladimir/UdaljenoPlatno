package Common;

import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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
        canvas.getGraphicsContext2D().setFill(Paint.valueOf("white"));
        canvas.getGraphicsContext2D().fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
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
            canvas.getGraphicsContext2D().setLineCap(StrokeLineCap.ROUND);
            canvas.getGraphicsContext2D().strokeLine(x1, y1, x2, y2);
            canvas.getGraphicsContext2D().setLineCap(StrokeLineCap.SQUARE);
        });
    }

    public void drawRectOnCanvas(double x, double y, double w, double h) {
        Platform.runLater(() -> canvas.getGraphicsContext2D().strokeRect(x, y, w, h));
    }

    public void drawOvalOnCanvas(double x, double y, double w, double h) {
        Platform.runLater(()-> canvas.getGraphicsContext2D().strokeOval(x, y, w, h));
    }

    public void resetCanvas(){
        Platform.runLater(()-> canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight()));

    }

    public void changeStrokeColor(Paint color){
        Platform.runLater(()-> canvas.getGraphicsContext2D().setStroke(color));

    }

    public void changeLineWidth(double width){
        Platform.runLater(()-> canvas.getGraphicsContext2D().setLineWidth(width));

    }

    public String exportCanvasToBase64() throws InterruptedException {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        WritableImage writableImage = new WritableImage(width, height);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT); // ili Color.WHITE za pozadinu
            canvas.snapshot(params, writableImage);
            latch.countDown();
        });

        latch.await();

        try {
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            javafx.scene.image.PixelReader pixelReader = writableImage.getPixelReader();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    javafx.scene.paint.Color fxColor = pixelReader.getColor(x, y);
                    int a = (int) Math.round(fxColor.getOpacity() * 255);
                    int r = (int) Math.round(fxColor.getRed() * 255);
                    int g = (int) Math.round(fxColor.getGreen() * 255);
                    int b = (int) Math.round(fxColor.getBlue() * 255);
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    bufferedImage.setRGB(x, y, argb);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            baos.flush();

            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
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
        return roomName;
    }
}
