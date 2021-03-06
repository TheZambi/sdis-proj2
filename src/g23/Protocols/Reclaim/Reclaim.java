package g23.Protocols.Reclaim;

import g23.*;
import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Protocols.Backup.BackupMessageSender;

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
    }

    private void removeFile(Map.Entry<Long, FileInfo> stringFileInfoEntry) {

        System.out.println("REMOVING FILE: " + stringFileInfoEntry.getValue().getHash());
        FileInfo fileInfo = stringFileInfoEntry.getValue();

        String[] msgArgs = {
                String.valueOf(this.peer.getId()),
                String.valueOf(fileInfo.getHash()),
                String.valueOf(fileInfo.getDesiredReplicationDegree()),
                "1"
        };

        System.out.println(msgArgs);

        Message msgToSend = new Message(MessageType.REMOVED, msgArgs, null);

        try {
            Files.delete(Path.of("backup/" + stringFileInfoEntry.getKey()));

            System.out.println("DELETED FILE " + stringFileInfoEntry.getKey() + " BECAUSE OF RECLAIM");

            (new IDeletedMessageSender(this.peer, msgToSend.getFileId())).run();

            (new BackupMessageSender(this.peer, msgToSend)).run();

            peer.removeSpace(fileInfo.getSize());
            peer.getStoredFiles().remove(stringFileInfoEntry.getKey());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
