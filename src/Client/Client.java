package Client;

public class Client {

    static ClientGUI gui;

    public static void main(String[] args) throws Exception {
        gui = new ClientGUI();
        gui.start(args);
    }
}
