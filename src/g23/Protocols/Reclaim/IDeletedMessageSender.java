package g23.Protocols.Reclaim;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Peer;
import g23.PeerInfo;
import g23.SSLEngine.SSLClient;
import java.io.ByteArrayOutputStream;
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


        SSLClient sslClient = null;

        try {
            long fileId = message.getFileId();
            FileInfo fi = this.peer.getStoredFiles().get(fileId);
            PeerInfo pi = fi.getPeerInfo();

            sslClient = new SSLClient(pi.getAddress());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush();
            byte[] msg = bos.toByteArray();
            sslClient.write(msg, msg.length);
            bos.close();

            System.out.println("SENT IDELETED (" + message.getFileId() + ") TO " + pi.getAddress().getAddress() + ":" + pi.getAddress().getPort());

            sslClient.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
