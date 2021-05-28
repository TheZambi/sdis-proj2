package g23.Protocols;

import g23.FileInfo;
import g23.MessageSender;
import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Peer;
import g23.PeerInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Reclaim implements Runnable {
    private final Peer peer;
    private final long space;

    public Reclaim(Peer peer, long space) {
        this.peer = peer;
        this.space = space;
    }

    @Override
    public void run() {
        System.out.println(peer.getCurrentSpace());
        System.out.println("RECLAIMING TO " + space);
        peer.getStoredFiles().entrySet().stream().sorted(Map.Entry.comparingByValue())
                .takeWhile(m -> peer.getCurrentSpace() > space)
                .forEach(this::removeFile);
        peer.setMaxSpace(space);
        //peer.getOngoing().remove("reclaim-" + space);
    }

    private void removeFile(Map.Entry<Long, FileInfo> stringFileInfoEntry) {

        System.out.println("REMOVING FILE: " + stringFileInfoEntry.getValue().getHash());
        FileInfo fileInfo = stringFileInfoEntry.getValue();

        String[] msgArgs = {
                String.valueOf(this.peer.getId()),
                String.valueOf(fileInfo.getHash()),
                String.valueOf(fileInfo.getDesiredReplicationDegree()),
                String.valueOf(1)
        };

        Message msgToSend = new Message(MessageType.REMOVED, msgArgs, null);

        try {
            File fileToRemove = new File("backup/" + stringFileInfoEntry.getKey());

            if (fileToRemove.delete()) {
                System.out.println("SENDING REMOVED " + stringFileInfoEntry.getValue().getHash());
                peer.removeSpace(fileInfo.getSize());
                peer.getStoredFiles().remove(stringFileInfoEntry.getKey());

                this.peer.getProtocolPool().execute(new MessageSender(msgToSend, this.peer.getSuccessor().getAddress().getAddress(), this.peer.getSuccessor().getAddress().getPort()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
