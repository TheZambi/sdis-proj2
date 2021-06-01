package g23.Protocols.Backup;

import g23.Messages.Message;
import g23.Peer;
import g23.PeerInfo;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
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

        SSLSocket socket = null;
        try {
            long fileId = message.getFileId();
            PeerInfo succ = this.peer.findSuccessor(fileId);

            if (succ.getId() != this.peer.getId()) {
                socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(succ.getAddress().getAddress(), succ.getAddress().getPort());
                socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
            } else {
                for (int i = 0; i < this.peer.getSuccessors().size(); i++) {
                    try {
                        socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(this.peer.getSuccessors().get(i).getAddress().getAddress(), this.peer.getSuccessor().getAddress().getPort());
                        socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

                        break;
                    } catch (IOException e) {
                        if (i == 0) {
                            this.peer.getFingerTable().set(0, this.peer.getPeerInfo());
                        }
                        if (i == this.peer.getSuccessors().size() - 1) {
                            System.out.println("Couldn't connect with any successor to send BACKUP.");
                            return;
                        }
                    }
                }
            }

            if (socket == null) {
                return;
            }

            System.out.println("Sending " + this.message.getType().toString() + " of file " + fileId + " to " + socket.getPort());

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
