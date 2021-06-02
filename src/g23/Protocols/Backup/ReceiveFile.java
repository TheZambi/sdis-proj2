package g23.Protocols.Backup;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Peer;
import g23.PeerInfo;

import g23.SSLEngine.SSLClient;
import g23.SSLEngine.SSLFinishedReadingException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
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

//                    SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(message.getAddress(), message.getPort());
//                    socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
                    SSLClient fromServer = new SSLClient(new InetSocketAddress(message.getAddress(), message.getPort()));
//                    fromServer.doHandshake();

                    String[] msgArgs = {
                            String.valueOf(this.peer.getId()),
                            String.valueOf(message.getFileId())
                    };
                    Message fileRequest = new Message(MessageType.IWANT, msgArgs, null);


                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(fileRequest);
                    oos.flush();
                    byte[] msg = bos.toByteArray();
                    fromServer.write(msg, msg.length);
                    bos.close();

                    WritableByteChannel toNewFile = Channels.newChannel(Files.newOutputStream(Path.of("backup/" + key)));

                    byte[] buffer = new byte[50000];

                    int bytesRead = 0;
                    while (true) {
                        try {
                            bytesRead = fromServer.read(buffer);
                        } catch (SSLFinishedReadingException e) {
                            break;
                        }
                        ByteBuffer b_buffer;
                        b_buffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                        toNewFile.write(b_buffer);
                    }
                    fromServer.shutdown();

                    toNewFile.close();

                    peer.addSpace(message.getFileSize());

                    System.out.println("STORED FILE " + key);

                    // Add to out storedfiles map
                    FileInfo fi = new FileInfo(null, this.message.getFileId(), this.message.getReplicationDegree(), new PeerInfo(new InetSocketAddress(this.message.getAddress(), this.message.getPort()), this.message.getSenderId()));
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
        System.out.println("Current Replication Degree: " + message.getCurrentReplicationDegree());
        if (this.message.getCurrentReplicationDegree() > 0) {
            (new Backup(this.peer, this.message)).run();
        }
    }
}
