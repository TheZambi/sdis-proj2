package g23.Protocols.Restore;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Peer;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReceiveRestoreFile {
    private final Peer peer;
    private final Message message;
    private final SSLSocket socket;

    public ReceiveRestoreFile(Peer peer, Message message, SSLSocket socket) {
        this.peer = peer;
        this.message = message;
        this.socket = socket;
    }

    public void handleMessage() {
        long fileId = message.getFileId();
        FileInfo fileToBeRestored = this.peer.getFiles().get(fileId); // TODO maybe check for null

        if (this.peer.getFilesToRestore().contains(fileId)) {

            try {
                //Receiving and storing the file
                ReadableByteChannel fromInitiator = Channels.newChannel(socket.getInputStream());
                Files.deleteIfExists(Path.of("restore/" + fileToBeRestored.getPath()));
                Path newFile = Files.createFile(Path.of("restore/" + fileToBeRestored.getPath()));
                WritableByteChannel toNewFile = Channels.newChannel(Files.newOutputStream(newFile));
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                while (fromInitiator.read(buffer) > 0 || buffer.position() > 0) {
                    buffer.flip();
                    toNewFile.write(buffer);
                    buffer.compact();
                }
                toNewFile.close();
                fromInitiator.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Removing from out set
            this.peer.getFilesToRestore().remove(fileId);

        } else {
            System.err.println("A file not requested for restore is being restored");
        }

        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
