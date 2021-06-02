package g23.Protocols.Restore;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Peer;
import g23.SSLEngine.SSLFinishedReadingException;
import g23.SSLEngine.SSLServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReceiveRestoreFile {
    private final Peer peer;
    private final Message message;
    private final SSLServer sslServer;

    public ReceiveRestoreFile(Peer peer, Message message, SSLServer sslServer) {
        this.peer = peer;
        this.message = message;
        this.sslServer = sslServer;
    }

    public void handleMessage() {
        long fileId = message.getFileId();
        FileInfo fileToBeRestored = this.peer.getFiles().get(fileId); // TODO maybe check for null

        if (this.peer.getFilesToRestore().contains(fileId)) {

            try {

                //Receiving and storing the file

                Files.deleteIfExists(Path.of("restore/" + fileToBeRestored.getPath()));
                WritableByteChannel toNewFile = Channels.newChannel(Files.newOutputStream(Path.of("restore/" + fileToBeRestored.getPath())));

                byte[] buffer = new byte[50000];

                int bytesRead = 0;
                while (true) {
                    try {
                        bytesRead = sslServer.read(buffer);
                    } catch (SSLFinishedReadingException e) {
                        break;
                    }
                    ByteBuffer b_buffer;
                    b_buffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                    toNewFile.write(b_buffer);
                }
                System.out.println("RECEIVED FILE DATA RESTORE (" + fileId + ")");

                toNewFile.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            //Removing from out set
            this.peer.getFilesToRestore().remove(fileId);

        } else {
            System.err.println("A file not requested for restore is being restored");
        }

    }
}
