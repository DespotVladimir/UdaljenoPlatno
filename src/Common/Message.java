package Common;

import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.Arrays;

public class Message{

    public static byte[] getBytes(Shape shape,double v1,double v2,double v3,double v4,double width,Color color){
        String returnBytes = String.format("CRTAJ|%s;%.2f;%.2f;%.2f;%.2f;%f;%f;%f;%f;%f\n",
                shape.toString(),v1,v2,v3,v4,color.getRed(),color.getGreen(),color.getBlue(),color.getOpacity(),width);
        return returnBytes.getBytes();
    }

    double x1, y1, x2, y2;
    double width;

    Shape shape;
    Color color;

    public Message(Shape shape,double x1,double y1,double x2,double y2,double width,Color color) {
        this.shape = shape;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;
        this.width = width;
    }

    public Message(byte[] message) throws IOException {
        this(new String(message));
    }

    public Message(String message) throws IOException {
        String[] parts = (message.split("\\|")[0]).split(";");
        try{
            this.shape = Shape.getShapeByName(parts[0]);
            this.x1 = Double.parseDouble(parts[1]);
            this.y1 = Double.parseDouble(parts[2]);
            this.x2 = Double.parseDouble(parts[3]);
            this.y2 = Double.parseDouble(parts[4]);
            this.color = new Color(
                    Double.parseDouble(parts[5]),
                    Double.parseDouble(parts[6]),
                    Double.parseDouble(parts[7]),
                    Double.parseDouble(parts[8])
            );
            this.width = Double.parseDouble(parts[9]);
        }
        catch (Exception e){
            e.printStackTrace();
            System.err.println(Arrays.toString(parts));
            throw new IOException(e);
        }
    }




    public byte[] getBytes() {
        return Message.getBytes(this.shape,this.x1,this.y1,this.x2,this.y2,this.width,this.color);
    }

    public double[] getCoordinates() {
        return new double[]{this.x1,this.y1,this.x2,this.y2};
    }

    public Shape getShape() {
        return shape;
    }

    public Color getColor() {
        return color;
    }

    public double getWidth() {
        return width;
    }

    public void setCoordinates(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public String getFormatedMessage(){
        return String.format("CRTAJ|%s;%.2f;%.2f;%.2f;%.2f;%f;%f;%f;%f;%f\n",
                shape.toString(),x1,y1,x2,y2,color.getRed(),color.getGreen(),color.getBlue(),color.getOpacity(),width);

    }

    @Override
    public String toString() {
        return "Message{" +
                "v1=" + x1 +
                ", v2=" + y1 +
                ", v3=" + x2 +
                ", v4=" + y2 +
                ", shape=" + shape +
                ", color=" + color +
                '}';
    }
}
