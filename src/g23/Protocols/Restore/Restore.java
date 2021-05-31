package g23.Protocols.Restore;

import g23.*;
import g23.Messages.Message;
import g23.Messages.MessageType;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.stream.Stream;

public class Restore implements Runnable {
    private final Peer peer;
    private final String path;

    public Restore(Peer peer, String path) {
        this.peer = peer;
        this.path = path;
    }

    @Override
    public void run() {

        //Check if we backed up this file
        Stream<Map.Entry<Long, FileInfo>> matches = this.peer.getFiles().entrySet().stream().filter(f -> f.getValue().getPath().equals(path));
        FileInfo file = matches.findFirst().get().getValue();
        if (file == null) {
            System.err.println("File not found in backup system");
            return;
        }

        try {
            long fileId = file.getHash();
            PeerInfo succID = this.peer.findSuccessor(fileId);

            String[] msgArgs = {
                    String.valueOf(this.peer.getId()),
                    String.valueOf(fileId),
                    String.valueOf(this.peer.getAddress().getAddress().getHostAddress()),
                    String.valueOf(this.peer.getAddress().getPort())
            };
            Message msgToSend = new Message(MessageType.GETFILE, msgArgs, null);

            SSLSocket socket = null;
            if (succID.getId() != this.peer.getId()) {
                socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(succID.getAddress().getAddress(), succID.getAddress().getPort());
                socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
            } else {

                for (int i = 0; i < this.peer.getSuccessors().size(); i++) {
                    try {
                        socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(this.peer.getSuccessor().getAddress().getAddress(), this.peer.getSuccessor().getAddress().getPort());
                        socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
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

            }

            if (socket == null) {
                return;
            }

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(msgToSend);

            //Add file to set of files to restore so that when we receive a conection it can be validated
            //TODO maybe make a timeout if the file doesnt come
            this.peer.getFilesToRestore().add(fileId);

//            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
