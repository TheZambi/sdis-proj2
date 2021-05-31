package g23.Protocols.Reclaim;

import g23.RemovedMessagePropSender;
import g23.Messages.Message;
import g23.Peer;
import g23.Protocols.Backup.Backup;

public class ReceiveRemoved {
    private final Peer peer;
    private final Message message;

    public ReceiveRemoved(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    public void handleMessage() {
        try {
            if (this.peer.findSuccessor(message.getFileId()).getId() == this.peer.getId()) {
                if (message.isSeen()) {
                    System.out.println("Found my message, stopping the propagation");
                    return;
                }
                message.seeMessage();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (this.peer.getFiles().containsKey(message.getFileId())) { //peer is owner of the file
            System.out.println("RECEIVED REMOVED " + message.getFileId() + ". I AM OWNER");

            String filePath = this.peer.getFiles().get(message.getFileId()).getPath();
            (new Backup(this.peer, filePath, message.getReplicationDegree(), message.getCurrentReplicationDegree())).run();

        } else if (this.peer.getStoredFiles().containsKey(message.getFileId())) { //peer has a backup of this file

            System.out.println("RECEIVED REMOVED " + message.getFileId() + ". I HAVE A BACKUP");
            (new Backup(this.peer, message.getFileId(), message.getReplicationDegree(), message.getCurrentReplicationDegree())).run();

        } else { // send message to successor

            System.out.println("RECEIVED REMOVED " + message.getFileId() + ". FORWARDING...");
            (new RemovedMessagePropSender(message, this.peer)).run();
        }
    }
}
