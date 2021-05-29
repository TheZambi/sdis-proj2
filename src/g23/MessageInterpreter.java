package g23;

import g23.Messages.Message;
import g23.Protocols.DeleteFile;
import g23.Protocols.ReceiveFile;
import g23.Protocols.Restore.ReceiveRestoreFile;
import g23.Protocols.Restore.SendRestoreFile;
import g23.Protocols.ReceiveRemoved;
import g23.Protocols.SendFile;

import javax.net.ssl.SSLSocket;
import java.io.ObjectInputStream;

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
                    //we will receive a file we requested to restore
                case RESTOREFILE:
                    (new ReceiveRestoreFile(this.peer, msg, this.socket)).handleMessage();
                    break;
                    //Request to restore a file
                case GETFILE:
                    (new SendRestoreFile(this.peer, msg)).handleMessage();
                    break;
                case REMOVED:
                    (new ReceiveRemoved(this.peer, msg)).handleMessage();
                    break;
                case DELETE:
                    new DeleteFile(this.peer, msg).handleMessage();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
