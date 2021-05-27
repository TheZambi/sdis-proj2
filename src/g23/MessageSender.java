package g23;

import g23.Messages.Message;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class MessageSender implements Runnable {

    private PeerInfo peer;
    private Message message;


    public MessageSender(PeerInfo peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(this.peer.getAddress().getAddress(), this.peer.getAddress().getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
        ) {
            out.writeObject(this.message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
