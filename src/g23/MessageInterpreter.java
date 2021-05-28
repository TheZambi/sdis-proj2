package g23;

import g23.Messages.Message;
import g23.Protocols.ReceiveFile;
import g23.Protocols.ReceiveRemoved;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Arrays;

public class MessageInterpreter implements Runnable {
    private Peer peer;
    private Socket socket;

    public MessageInterpreter(Peer peer, Socket socket) {
        this.peer = peer;
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            Message msg = (Message) ois.readObject();

            switch (msg.getType()) {
                case PUTFILE :
                    new ReceiveFile(this.peer, msg).handleMessage();
                    break;
                case REMOVED:
                    new ReceiveRemoved(this.peer, msg).handleMessage();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
