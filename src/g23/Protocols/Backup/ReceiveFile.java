package g23.Protocols.Backup;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Peer;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.stream.Stream;

public class ReceiveFile {
    private final Peer peer;
    private final Message message;

    public ReceiveFile(Peer peer, Message message) {
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

        //we are not yet storing this file and we are not the owner of the file
        if (!peer.getStoredFiles().containsKey(key)) {

            // We are the ones requesting the backup
            Stream<FileInfo> fileInThisPeer = peer.getFiles().values().stream().filter(f -> f.getHash() == message.getFileId());
            if (fileInThisPeer.count() > 0) {
                (new Backup(this.peer, this.message)).run();
                return;
            }

            try {
                // will store if there is enough space in the peer
                if (peer.getRemainingSpace() >= message.getFileSize()) {

                    System.out.println("RECEIVING File " + key);

                    SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(message.getAddress(), message.getPort());
                    socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                    String[] msgArgs = {
                            String.valueOf(this.peer.getId()),
                            String.valueOf(message.getFileId())
                    };
                    Message fileRequest = new Message(MessageType.IWANT, msgArgs, null);
                    oos.writeObject(fileRequest);

                    ReadableByteChannel fromInitiator = Channels.newChannel(socket.getInputStream());

                    Path newFile = Files.createFile(Path.of("backup/" + key));

                    WritableByteChannel toNewFile = Channels.newChannel(Files.newOutputStream(newFile));

                    ByteBuffer buffer = ByteBuffer.allocate(4096);

                    while (fromInitiator.read(buffer) > 0 || buffer.position() > 0) {
                        buffer.flip();
                        toNewFile.write(buffer);
                        buffer.compact();
                    }

                    toNewFile.close();
                    fromInitiator.close();

                    peer.addSpace(message.getFileSize());

                    System.out.println("STORED FILE " + key);

                    // Add to out storedfiles map
                    FileInfo fi = new FileInfo(null, this.message.getFileId(), this.message.getReplicationDegree(), this.peer.getPeerInfo());
                    this.peer.getStoredFiles().put(key, fi);
                    this.message.decrementCurrentReplication();
                } else {
                    System.out.println("NOT ENOUGH SPACE FOR FILE " + key);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            this.message.decrementCurrentReplication();
        }
        //If needed send the file to successor (decrements replication degree)
        System.out.println("Current Replication Degree: " + message.getReplicationDegree());
        if (this.message.getCurrentReplicationDegree() > 0) {
            (new Backup(this.peer, this.message)).run();
        }
    }
}
