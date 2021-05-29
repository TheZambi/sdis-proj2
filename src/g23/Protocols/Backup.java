package g23.Protocols;

import g23.BackupMessageSender;
import g23.FileInfo;
import g23.Messages.*;
import g23.Peer;
import g23.PeerInfo;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
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
                if (!this.peer.getStoredFiles().containsKey(hash))
                    return;
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
                        // peer.getOngoing().remove("backup-" + path + "-" + replicationDegree);
                        System.err.println("Backup " + path + ": File doesn't exist or has size larger than 64GB");
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

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
                this.peer.getFiles().put(hash, new FileInfo(path, hash, currentReplicationDegree, replicationDegree, peer));

        } else {
            //Backup to successor until replication degree is reached
            try {
                PeerInfo successor = peer.getSuccessor();
                SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(successor.getAddress().getAddress(), successor.getAddress().getPort());
                socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("Sending BACKUP (propagation) to " + socket.getPort());
                oos.writeObject(this.message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
