package g23.Protocols.Delete;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Peer;
import g23.PeerInfo;
import g23.SSLEngine.SSLClient;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class Delete implements Runnable {
    private final Peer peer;
    private final String path;
    private final Message message;

    public Delete(Peer peer, String path) {
        this.peer = peer;
        this.path = path;
        this.message = null;
    }

    public Delete(Peer peer, Message message) {
        this.peer = peer;
        this.path = null;
        this.message = message;
    }

    @Override
    public void run() {

        if (this.message == null) {

            Stream<Map.Entry<Long, FileInfo>> matches = this.peer.getFiles().entrySet().stream().filter(f -> f.getValue().getPath().equals(path));

            AtomicBoolean deleted = new AtomicBoolean(false);

            matches.forEach((match) -> {
                Long hash = match.getValue().getHash();
                String[] msgArgs = {String.valueOf(this.peer.getId()), String.valueOf(hash), String.valueOf(match.getValue().getDesiredReplicationDegree())};
                Message deleteMsg = new Message(MessageType.DELETE, msgArgs, null);

                try {
                    long fileId = Peer.getFileId(this.path, this.peer.getId());
                    PeerInfo succID = this.peer.findSuccessor(fileId);

                    SSLClient sslClient = null;

                    if (succID.getId() != this.peer.getId()) {
                        sslClient = new SSLClient(succID.getAddress());
                    } else {

                        for(int i=0; i<this.peer.getSuccessors().size();i++) {
                            try {
                                sslClient = new SSLClient(this.peer.getSuccessors().get(i).getAddress());

                                break;
                            } catch(Exception e)
                            {
                                if(i == 0)
                                    this.peer.getFingerTable().set(0,this.peer.getPeerInfo());
                                if(i == this.peer.getSuccessors().size() - 1)
                                {
                                    System.out.println("Couldn't connect with any successor to send DELETE.");
                                    return;
                                }
                            }
                        }
                    }
                    if(sslClient == null)
                        return;

                    this.peer.getFiles().remove(match.getKey());
                    deleted.set(true);

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(deleteMsg);
                    oos.flush();
                    byte[] msg = bos.toByteArray();
                    sslClient.write(msg, msg.length);
                    bos.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
        } else {
            System.out.println("REACHED FOR");
            for(int i=0; i<this.peer.getSuccessors().size();i++) {
                try {
                    PeerInfo successor = peer.getSuccessors().get(i);
                    SSLClient sslClient = new SSLClient(successor.getAddress());

                    System.out.println("Sending DELETE (propagation) to " + successor.getAddress().getPort());

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(this.message);
                    oos.flush();
                    byte[] msg = bos.toByteArray();
                    sslClient.write(msg, msg.length);
                    bos.close();

                    break;
                } catch (Exception e) {
                    if(i == 0)
                        this.peer.getFingerTable().set(0,this.peer.getPeerInfo());
                    if(i == this.peer.getSuccessors().size() - 1)
                    {
                        System.out.println("Couldn't connect with any successor to send DELETE.");
                        return;
                    }
                }
            }
        }
    }
}
