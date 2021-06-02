package g23;

import g23.Messages.Message;
import g23.Protocols.Delete.DeleteFile;
import g23.Protocols.Backup.ReceiveFile;
import g23.Protocols.Restore.ReceiveRestoreFile;
import g23.Protocols.Restore.SendRestoreFile;
import g23.Protocols.Reclaim.ReceiveRemoved;
import g23.Protocols.Backup.SendFile;
import g23.SSLEngine.SSLEngineOrchestrator;
import g23.SSLEngine.SSLServer;

import javax.net.ssl.SSLSocket;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class MessageInterpreter implements Runnable {
    private Peer peer;
    private SSLServer sslServer;

    public MessageInterpreter(Peer peer, SSLServer sslServer) {
        this.peer = peer;
        this.sslServer = sslServer;
//        this.sslServer.doHandshake();
    }

    @Override
    public void run() {
        byte[] readMessage = new byte[40000];
        int readBytes;

        try {
            readBytes = sslServer.read(readMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(readMessage);


        try (ObjectInputStream ois = new ObjectInputStream(bis)) {
            Message msg = (Message) ois.readObject();
            System.out.println(msg.getType());

//            Message msg = (Message) ois.readObject();
//
            switch (msg.getType()) {
                case PUTFILE:
                    (new ReceiveFile(this.peer, msg)).handleMessage();
                    break;
                case IWANT:
                    (new SendFile(this.peer, msg, sslServer)).handleMessage();
                    break;
                //we will receive a file we requested to restore
//                case RESTOREFILE:
//                    (new ReceiveRestoreFile(this.peer, msg, this.socket)).handleMessage();
//                    break;
                //Request to restore a file
//                case GETFILE:
//                    (new SendRestoreFile(this.peer, msg)).handleMessage();
//                    break;
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
        sslServer.shutdown();
    }
//    public MessageInterpreter(Peer peer, SSLSocket socket) {
//        this.peer = peer;
//        this.socket = socket;
//    }
//
//    @Override
//    public void run() {
//        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
//            Message msg = (Message) ois.readObject();
//
//            switch (msg.getType()) {
//                case PUTFILE:
//                    (new ReceiveFile(this.peer, msg)).handleMessage();
//                    break;
//                case IWANT:
//                    (new SendFile(this.peer, msg, this.socket)).handleMessage();
//                    break;
//                //we will receive a file we requested to restore
//                case RESTOREFILE:
//                    (new ReceiveRestoreFile(this.peer, msg, this.socket)).handleMessage();
//                    break;
//                //Request to restore a file
//                case GETFILE:
//                    (new SendRestoreFile(this.peer, msg)).handleMessage();
//                    break;
//                case REMOVED:
//                    (new ReceiveRemoved(this.peer, msg)).handleMessage();
//                    break;
//                case DELETE:
//                    new DeleteFile(this.peer, msg).handleMessage();
//                    break;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
