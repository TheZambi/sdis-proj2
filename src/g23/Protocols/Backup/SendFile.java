package g23.Protocols.Backup;

import g23.Messages.Message;
import g23.Peer;
import g23.SSLEngine.SSLServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class SendFile {
    private final Peer peer;
    private final Message message;
    private final SSLServer toClient;

    public SendFile(Peer peer, Message message, SSLServer toClient) {
        this.peer = peer;
        this.message = message;
        this.toClient = toClient;
    }

    public void handleMessage() {
        Path filePath;

        if (this.peer.getFiles().containsKey(message.getFileId())) { //peer is owner of file
            filePath = Path.of(this.peer.getFiles().get(message.getFileId()).getPath());

        } else if (this.peer.getStoredFiles().containsKey(message.getFileId())) { //peer has a backup of file
            filePath = Path.of("backup/" + message.getFileId());

        } else
            return;

        if (!Files.exists(filePath))
            return;

        try {
            ReadableByteChannel fromFile = Channels.newChannel(Files.newInputStream(filePath));
            ByteBuffer buffer = ByteBuffer.allocate(4096);

            int bytesRead = 0;
            while ((bytesRead = fromFile.read(buffer)) > 0 || buffer.position() > 0) {
                buffer.flip();
                toClient.write(buffer.array(), bytesRead);
                buffer.clear();
            }

            System.out.println("SENT FILE " + message.getFileId() + " DATA TO " + message.getSenderId());

            fromFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        this.peer.getFilesStoredInPeers().get(this.message.getFileId()).add(this.message.getSenderId());
    }
}
