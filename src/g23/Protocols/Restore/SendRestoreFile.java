package g23.Protocols.Restore;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Peer;
import g23.PeerInfo;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;

public class SendRestoreFile {
    private final Peer peer;
    private final Message message;

    public SendRestoreFile(Peer peer, Message message) {
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

        FileInfo fileInfo = this.peer.getStoredFiles().get(message.getFileId());
        System.out.println(fileInfo);
        if (fileInfo == null || !Files.exists(Path.of("backup/" + fileInfo.getHash()))) {
            //File does not exist so we forward the request
            for(int i=0; i<peer.getSuccessors().size(); i++){
                try {
                    PeerInfo successor = peer.getSuccessors().get(i);
                    SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(successor.getAddress().getAddress(), successor.getAddress().getPort());
                    socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    System.out.println("Sending Restore (propagation) to " + socket.getPort());
                    oos.writeObject(this.message);
                    break;
                } catch (IOException e) {
                    if (i == 0) {
                        this.peer.getFingerTable().set(0, this.peer.getPeerInfo());
                    }
                    if (i == this.peer.getSuccessors().size() - 1) {
                        System.out.println("Couldn't connect with any successor to send DELETE.");
                        return;
                    }
                }
            }

        } else {
            System.out.println("FOUND FILE TO BACKUP");
            try {
                String[] msgArgs = {
                        String.valueOf(this.peer.getId()),
                        String.valueOf(message.getFileId())
                };

                Message msg = new Message(MessageType.RESTOREFILE, msgArgs, null);

                //Creating the socket to the file requester
                SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(message.getAddress(), message.getPort());
                socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(msg);


                WritableByteChannel toPeer = Channels.newChannel(socket.getOutputStream());
                ReadableByteChannel fromFile = Channels.newChannel(Files.newInputStream(Path.of("backup/" + fileInfo.getHash())));
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                while (fromFile.read(buffer) > 0 || buffer.position() > 0) {
                    buffer.flip();
                    toPeer.write(buffer);
                    buffer.compact();
                }

                toPeer.close();
                fromFile.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
