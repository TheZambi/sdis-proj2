package g23.Protocols.Reclaim;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Peer;
import g23.PeerInfo;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ObjectOutputStream;

public class IDeletedMessageSender implements Runnable {

    Peer peer;
    Long deletedFile;

    public IDeletedMessageSender(Peer peer, Long deletedFile) {
        this.peer = peer;
        this.deletedFile = deletedFile;
    }

    @Override
    public void run() {

        String[] msgArgs = {
                String.valueOf(this.peer.getId()),
                String.valueOf(deletedFile)
        };

        Message message = new Message(MessageType.IDELETED, msgArgs, null);


        SSLSocket socket = null;
        try {
            long fileId = message.getFileId();
            FileInfo fi = this.peer.getStoredFiles().get(fileId);
            PeerInfo pi = fi.getPeerInfo();

            socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(pi.getAddress().getAddress(), pi.getAddress().getPort());
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());



            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
