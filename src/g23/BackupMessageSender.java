package g23;

import g23.Messages.Message;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ObjectOutputStream;

/* For sending PUTFILE / REMOVED messages */
public class BackupMessageSender implements Runnable {
    private Peer peer;
    private Message message;
    public BackupMessageSender(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void run() {

        SSLSocket socket;
        try {
            long fileId = message.getFileId();
            PeerInfo succ = this.peer.findSuccessor(fileId);

            if (succ.getId() != this.peer.getId()) {
                socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(succ.getAddress().getAddress(), succ.getAddress().getPort());
            } else {
                socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(this.peer.getSuccessor().getAddress().getAddress(), this.peer.getSuccessor().getAddress().getPort());
            }
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

            System.out.println("Sending " + this.message.getType().toString() + " of file " + fileId + " to " + socket.getPort());

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
