package g23.Protocols;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Peer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.stream.Stream;

public class DeleteFile {
    private final Peer peer;
    private final Message message;

    public DeleteFile(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    public void handleMessage() throws RemoteException {
        if (this.peer.findSuccessor(message.getFileId()).getId() == this.peer.getId()) {
            if (message.isSeen()) {
                System.out.println("Found my message, stopping the propagation");
                return;
            }
            message.seeMessage();
        }
        long key = message.getFileId();
        System.out.println("DELETING File " + key);

        //we are storing this file
        if (peer.getStoredFiles().containsKey(key)) {

            // We are the ones requesting the backup
            Stream<FileInfo> fileInThisPeer = peer.getFiles().values().stream().filter(f -> f.getHash() == message.getFileId());
            if (fileInThisPeer.count() > 0) {
                (new Delete(this.peer, this.message)).run();
                return;
            }


            try {
                // will store if there is enough space in the peer

                synchronized (this) {
                    peer.removeSpace(Files.size(Paths.get("backup/" + key)));
                }


                System.out.println("Deleted FILE " + key);
                System.out.println("CURRENT REPLICATION: " + this.message.getCurrentReplicationDegree());
                //Storing the received file
                try {
                    Boolean deleted = Files.deleteIfExists(Path.of("backup/" + key));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Add to out storedfiles map
                this.peer.getStoredFiles().remove(key);
                this.message.decrementCurrentReplication();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //If needed send the file to successor (decrements replication degree)
        if (this.message.getCurrentReplicationDegree() > 0) {
            (new Delete(this.peer, this.message)).run();
        }
    }
}
