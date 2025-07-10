package Common;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class User {
    private String name;
    private String roomID;
    private SocketChannel socket;
    private ByteBuffer buffer;

    public User(String name, String roomID, SocketChannel socket) {
        this.name = name;
        this.roomID = roomID;
        this.socket = socket;
        buffer = ByteBuffer.allocate(1024);
    }

    public String getName() {
        return name;
    }

    public String getRoomID() {
        return roomID;
    }

    public SocketChannel getSocket() {
        return socket;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRoomID(String roomID) {
        this.roomID = roomID;
    }

    public void setSocket(SocketChannel socket) {
        this.socket = socket;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", roomID=" + roomID +
                ", socket=" + socket +
                '}';
    }
}
