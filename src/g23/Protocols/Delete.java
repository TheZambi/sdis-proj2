package g23.Protocols;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Peer;
import g23.PeerInfo;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class Delete implements Runnable {
    private final Peer peer;
    private final String path;
    private final Message message;

    public Delete(Peer peer, String path) {
        this.peer = peer;
        this.path = path;
        this.message = null;
    }

    public Delete(Peer peer, Message message) {
        this.peer = peer;
        this.path = null;
        this.message = message;
    }

    @Override
    public void run() {

        if (this.message == null) {

            Stream<Map.Entry<Long, FileInfo>> matches = this.peer.getFiles().entrySet().stream().filter(f -> f.getValue().getPath().equals(path));

            AtomicBoolean deleted = new AtomicBoolean(false);

            matches.forEach((match) -> {
                Long hash = match.getValue().getHash();
                String[] msgArgs = {String.valueOf(this.peer.getId()), String.valueOf(hash), String.valueOf(match.getValue().getDesiredReplicationDegree())};
                Message deleteMsg = new Message(MessageType.DELETE, msgArgs, null);

                try {
                    long fileId = Peer.getFileId(this.path, this.peer.getId());
                    PeerInfo succID = this.peer.findSuccessor(fileId);

                    SSLSocket socket;
                    if (succID.getId() != this.peer.getId()) {
                        socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(succID.getAddress().getAddress(), succID.getAddress().getPort());
                    } else {
                        socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(this.peer.getSuccessor().getAddress().getAddress(), this.peer.getSuccessor().getAddress().getPort());
                    }
                    socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

                    this.peer.getFiles().remove(match.getKey());
                    deleted.set(true);

                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(deleteMsg);


                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
        } else {
            try {
                PeerInfo successor = peer.getSuccessor();
                SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(successor.getAddress().getAddress(), successor.getAddress().getPort());
                socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("Sending DELETE (propagation) to " + socket.getPort());
                oos.writeObject(this.message);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }


    }
}
