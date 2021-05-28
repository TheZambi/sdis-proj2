package g23.Protocols;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Peer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.stream.Stream;

public class ReceiveFile {
    private final Peer peer;
    private final Message message;

    public ReceiveFile(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    public void handleMessage() throws RemoteException {
        if(this.peer.findSuccessor(message.getFileId()).getId() == this.peer.getId())
        {
            if(message.isSeen())
            {
                System.out.println("Found my message, stopping the propagation");
                return;
            }
            message.seeMessage();
        }
        long key = message.getFileId();
        System.out.println("RECEIVING File " + key);

        //we are not yet storing this file
        if (!peer.getStoredFiles().containsKey(key)) {

            // We are the ones requesting the backup
            Stream<FileInfo> fileInThisPeer = peer.getFiles().values().stream().filter(f -> f.getHash() == message.getFileId());
            if (fileInThisPeer.count() > 0) {
                (new Backup(this.peer, this.message)).run();
                return;
            }


            try {
                // will store if there is enough space in the peer
                boolean enough_space = false;

                synchronized (this) {
                    if (peer.getRemainingSpace() >= message.getBody().length) {
                        peer.addSpace(message.getBody().length);
                        enough_space = true;
                    }
                }

                if (enough_space) {
                    System.out.println("STORED FILE " + key);
                    System.out.println("CURRENT REPLICATION: " + this.message.getCurrentReplicationDegree());
                    //Storing the received file
                    try {
                        Path newFile = Files.createFile(Path.of("backup/" + key));
                        Files.write(newFile, message.getBody());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Add to out storedfiles map
                    FileInfo fi = new FileInfo(null, this.message.getFileId(), this.message.getCurrentReplicationDegree() , this.message.getReplicationDegree(), this.peer);
                    this.peer.getStoredFiles().put(key, fi);
                    this.message.decrementCurrentReplication();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //If needed send the file to successor (decrements replication degree)
        if (this.message.getCurrentReplicationDegree() > 0) {
            (new Backup(this.peer, this.message)).run();
        }
    }
}
