package g23.Protocols;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Peer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.stream.Stream;

public class ReceiveRemoved {
    private final Peer peer;
    private final Message message;

    public ReceiveRemoved(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    public void handleMessage() {
        if(this.peer.getStoredFiles().containsKey(message.getFileId())) { //peer is owner of the file
            String filePath = this.peer.getStoredFiles().get(message.getFileId()).getPath();
            (new Backup(this.peer, filePath, message.getReplicationDegree(), message.getCurrentReplicationDegree())).run();
        } else { // send message to successor

        }
    }
}
