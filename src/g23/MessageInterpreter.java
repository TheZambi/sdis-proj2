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
    }

    @Override
    public void run() {
        byte[] readMessage = new byte[40000];

        try {
            sslServer.read(readMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(readMessage);


        try (ObjectInputStream ois = new ObjectInputStream(bis)) {
            Message msg = (Message) ois.readObject();

            switch (msg.getType()) {
                case PUTFILE:
                    System.out.println("RECEIVED PUTFILE (" + msg.getFileId() + ", size: " + msg.getFileSize() +
                            ", desired=" + msg.getReplicationDegree() + ", current=" + msg.getCurrentReplicationDegree() + ") FROM " + msg.getSenderId());
                    (new ReceiveFile(this.peer, msg)).handleMessage();
                    break;
                case IWANT:
                    System.out.println("RECEIVED IWANT (" + msg.getFileId() + ") FROM " + msg.getSenderId());
                    (new SendFile(this.peer, msg, sslServer)).handleMessage();
                    break;
                //we will receive a file we requested to restore
                case RESTOREFILE:
                    System.out.println("RECEIVED RESTOREFILE (" + msg.getFileId() + ") FROM " + msg.getSenderId());
                    (new ReceiveRestoreFile(this.peer, msg, sslServer)).handleMessage();
                    break;
                //Request to restore a file
                case GETFILE:
                    System.out.println("RECEIVED GETFILE (" + msg.getFileId() + ") FROM " + msg.getSenderId());
                    (new SendRestoreFile(this.peer, msg)).handleMessage();
                    break;
                case REMOVED:
                    System.out.println("RECEIVED REMOVED (" + msg.getFileId() + ") FROM " + msg.getSenderId());
                    (new ReceiveRemoved(this.peer, msg)).handleMessage();
                    break;
                case DELETE:
                    System.out.println("RECEIVED DELETE (" + msg.getFileId() + ") FROM " + msg.getSenderId());
                    new DeleteFile(this.peer, msg).handleMessage();
                    break;
                case IDELETED:
                    System.out.println("RECEIVED IDELETED (" + msg.getFileId() + ") FROM " + msg.getSenderId());
                    this.peer.getFilesStoredInPeers().get(msg.getFileId()).remove(msg.getSenderId());
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
