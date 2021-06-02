package g23.Protocols.Backup;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Peer;
import g23.PeerInfo;
import g23.SSLEngine.SSLClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class Backup implements Runnable {
    private final Peer peer;
    private final String path;
    private final int replicationDegree;
    private final int currentReplicationDegree;
    private final Message message;
    private long hash;


    public Backup(Peer peer, String path, int replicationDegree, int currentReplicationDegree) {
        this.peer = peer;
        this.path = path;
        this.replicationDegree = replicationDegree;
        this.currentReplicationDegree = currentReplicationDegree;
        this.message = null;
        this.hash = -1;
    }

    public Backup(Peer peer, long hash, int replicationDegree, int currentReplicationDegree) {
        this.peer = peer;
        this.hash = hash;
        this.replicationDegree = replicationDegree;
        this.currentReplicationDegree = currentReplicationDegree;
        this.message = null;
        this.path = null;
    }

    public Backup(Peer peer, Message message) {
        this.peer = peer;
        this.path = null;
        this.message = message;
        this.replicationDegree = message.getReplicationDegree();
        this.currentReplicationDegree = message.getCurrentReplicationDegree();
        this.hash = -1;
    }

    @Override
    public void run() {

        if (message == null) {
            long fileSize = 0;
            Path filePath;

            if (path == null) { //peer has a backup of the file
                if (!this.peer.getStoredFiles().containsKey(hash)){
                    return;
                }
                filePath = Path.of("backup/" + hash);
                try {
                    fileSize = Files.size(filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else { // peer is OWNER of the file
                filePath = Path.of(path);

                try {
                    fileSize = Files.size(filePath);
                    if (!Files.exists(filePath) || Files.isDirectory(filePath) || fileSize > 64000000000L) {
                        System.err.println("Backup " + path + ": File doesn't exist or has size larger than 64GB");
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
;
            if (hash == -1) //OWNER
                hash = Peer.getFileId(path, peer.getId());

            String[] msgArgs = {
                    String.valueOf(this.peer.getId()),
                    String.valueOf(hash),
                    String.valueOf(replicationDegree),
                    String.valueOf(currentReplicationDegree),
                    String.valueOf(fileSize),
                    String.valueOf(this.peer.getAddress().getAddress().getHostAddress()),
                    String.valueOf(this.peer.getAddress().getPort())
            };

            Message msgToSend = new Message(MessageType.PUTFILE, msgArgs, null);
            (new BackupMessageSender(this.peer, msgToSend)).run();

            if (path != null) //OWNER
                this.peer.getFiles().put(hash, new FileInfo(path, hash, replicationDegree, peer.getPeerInfo()));

        } else {
            //Backup to successor until replication degree is reached
            for (int i = 0; i < this.peer.getSuccessors().size(); i++) {
                try {
                    PeerInfo successor = this.peer.getSuccessors().get(i);

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    System.out.println("Sending BACKUP (propagation) to " + successor.getAddress().getPort());
                    oos.writeObject(this.message);
                    oos.flush();
                    byte[] msg = bos.toByteArray();
                    SSLClient toSendMsg = new SSLClient(successor.getAddress());
                    toSendMsg.write(msg, msg.length);
                    bos.close();

                    //Managed to propagate to a successor.
                    break;
                } catch (IOException e) {
                    if (i == 0)
                        this.peer.getFingerTable().set(0, this.peer.getPeerInfo());
                    if (i == this.peer.getSuccessors().size() - 1)
                        System.out.println("Couldn't find an active successor, stopping BACKUP propagation");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
