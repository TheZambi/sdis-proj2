package g23.Protocols;

import g23.*;
import g23.Messages.Message;
import g23.Messages.MessageType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ReceiveChunk {
    private final Peer peer;
    private final Message message;
    private final Socket socket;

    public ReceiveChunk(Peer peer, Message message, Socket socket) {
        this.peer = peer;
        this.message = message;
        this.socket = socket;
    }


    public void handleMessage() {
        String key = message.getFileId() + "-" + message.getChunkNumber();
        System.out.println("RECEIVING CHUNK " + key);

        if (!peer.getChunks().containsKey(key)) {

            Stream<FileInfo> fileInThisPeer = peer.getFiles().values().stream().filter(f -> f.getHash().equals(message.getFileId()));
            if (fileInThisPeer.count() > 0) {
                this.reply(false);
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
                    try (FileOutputStream out = new FileOutputStream("backup/" + key)) {
                        out.write(message.getBody());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Chunk c = new Chunk(message.getFileId(), message.getChunkNumber(), message.getReplicationDegree(), message.getBody().length);
                    c.addPeer(peer.getId()); //set itself as peer

                    this.peer.getChunks().put(key, c);

                    this.reply(true);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else { //if the peer already has the chunk
            this.reply(true);
        }

        // in case this peer is trying to backup this chunk (reclaim - the replication drops) that operation will be canceled
        // because another peer already sent it
        if (peer.getBackupsToSend().containsKey(key)) {
            peer.getBackupsToSend().get(key).cancel(false);
            peer.getBackupsToSend().remove(key);
        }
        this.reply(false);
    }

    private void reply(boolean stored) {
        Message reply = new Message(stored ? MessageType.STORED : MessageType.FAIL,
                new String[]{
                        String.valueOf(this.peer.getProtocolVersion()),
                        String.valueOf(this.peer.getId()), message.getFileId(),
                        String.valueOf(message.getChunkNumber())},
                null);

        try (BufferedOutputStream out = new BufferedOutputStream(this.socket.getOutputStream())) {
            byte[] toSend = message.toByteArray();
            out.write(toSend, 0, 64000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
