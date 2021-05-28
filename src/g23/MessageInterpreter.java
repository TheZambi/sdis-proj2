package g23;

import g23.Messages.Message;
import g23.Protocols.ReceiveFile;
import g23.Protocols.SendFile;

import javax.net.ssl.SSLSocket;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Arrays;

public class MessageInterpreter implements Runnable {
    private Peer peer;
    private SSLSocket socket;

    public MessageInterpreter(Peer peer, SSLSocket socket) {
        this.peer = peer;
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            Message msg = (Message) ois.readObject();

            switch (msg.getType()) {
                case PUTFILE :
                    (new ReceiveFile(this.peer, msg)).handleMessage();
                    break;
                case IWANT:
                    (new SendFile(this.peer, msg, this.socket)).handleMessage();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
