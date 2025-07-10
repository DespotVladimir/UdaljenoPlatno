package Common;

import javafx.scene.canvas.Canvas;

import java.util.ArrayList;
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
        canvas = new Canvas();
        users = new ArrayList<>();
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
