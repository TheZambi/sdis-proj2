package g23.Protocols;

import g23.FileInfo;
import g23.Messages.*;
import g23.Peer;
import g23.PeerInfo;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;


public class Backup implements Runnable {
    private final Peer peer;
    private final String path;
    private final int replicationDegree;
    private final int currentReplicationDegree;
    private final Message message;


    public Backup(Peer peer, String path, int replicationDegree, int currentReplicationDegree) {
        this.peer = peer;
        this.path = path;
        this.replicationDegree = replicationDegree;
        this.currentReplicationDegree = currentReplicationDegree;
        this.message = null;
    }

    public Backup(Peer peer, Message message) {
        this.peer = peer;
        this.path = null;
        this.message = message;
        this.replicationDegree = message.getReplicationDegree();
        this.currentReplicationDegree = message.getCurrentReplicationDegree();
    }

    @Override
    public void run() {

        if (message == null) {
            Path filePath = Path.of(path);
            try {
                if (!Files.exists(filePath) || Files.isDirectory(filePath) || Files.size(filePath) > 64000000000L) {
                    // peer.getOngoing().remove("backup-" + path + "-" + replicationDegree);
                    System.err.println("Backup " + path + ": File doesn't exist or has size larger than 64GB");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            long hash = Peer.getFileId(path, peer.getId());
            String[] msgArgs = {
                    String.valueOf(this.peer.getId()),
                    String.valueOf(hash),
                    String.valueOf(replicationDegree),
                    String.valueOf(currentReplicationDegree)
            };

            try {
                long fileId = Peer.getFileId(this.path, this.peer.getId());
                PeerInfo succID = this.peer.findSuccessor(fileId);

                System.out.println(succID);

                byte[] fileToSend = Files.readAllBytes(Path.of(this.path));
                Message msgToSend = new Message(MessageType.PUTFILE, msgArgs, fileToSend);

                Socket socket;
                if (succID.getId() != this.peer.getId()) {
                    socket = new Socket(succID.getAddress().getAddress(), succID.getAddress().getPort());
                } else {
                    socket = new Socket(this.peer.getSuccessor().getAddress().getAddress(), this.peer.getSuccessor().getAddress().getPort());
                }
                System.out.println(socket);
                System.out.println("Sending BACKUP to " + socket.getPort());
                System.out.println(msgToSend.getCurrentReplicationDegree());
                System.out.println("Created socket");
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(msgToSend);
                System.out.println("Finished Writing");

                this.peer.getFiles().put(hash, new FileInfo(path, hash, currentReplicationDegree, replicationDegree, peer));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //Backup to successor until replication degree is reached
            try {
                PeerInfo successor = peer.getSuccessor();
                Socket socket = new Socket(successor.getAddress().getAddress(), successor.getAddress().getPort());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                System.out.println("Sending BACKUP (propagation) to " + socket.getPort());

                oos.writeObject(this.message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        byte[] data;
//        int nRead = -1;
//        int nChunk = 0;
//        try (FileInputStream file = new FileInputStream(path)) {

        // read chunks

//            FileInfo fileInfo = new FileInfo(path, hash, replicationDegree, nChunk);
//            if(this.peer.getFiles().containsKey(hash)) {
//                fileInfo.setChunksPeers(this.peer.getFiles().get(hash).getChunksPeers());
//            }
//            this.peer.getFiles().put(hash, fileInfo);

//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
