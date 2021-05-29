package g23.Protocols;

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

public class SendFile {
    private Peer peer;
    private Message message;
    private SSLSocket socket;

    public SendFile(Peer peer, Message message, SSLSocket socket) {
        this.peer = peer;
        this.message = message;
        this.socket = socket;
    }

    public void handleMessage() {
        if(!this.peer.getFiles().containsKey(message.getFileId()))
            return;

        FileInfo fileInfo = this.peer.getFiles().get(message.getFileId());
        if(!Files.exists(Path.of(fileInfo.getPath())))
            return;

        try {
            WritableByteChannel toPeer = Channels.newChannel(socket.getOutputStream());
            ReadableByteChannel fromFile = Channels.newChannel(Files.newInputStream(Path.of(fileInfo.getPath())));
            ByteBuffer buffer = ByteBuffer.allocate(4096);

            while(fromFile.read(buffer) > 0 || buffer.position() > 0) {
                buffer.flip();
                toPeer.write(buffer);
                buffer.compact();
            }

            toPeer.close();
            fromFile.close();

        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
