package g23.Protocols;

import g23.*;
import g23.Messages.Message;
import g23.Messages.MessageType;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
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
        if(file == null) {
            System.err.println("File not found in backup system");
            return;
        }

        try {
            long fileId = file.getHash();
            PeerInfo succID = this.peer.findSuccessor(fileId);

            String[] msgArgs = {
                    String.valueOf(this.peer.getId()),
                    String.valueOf(fileId),
            };
            Message msgToSend = new Message(MessageType.GETFILE, msgArgs, null);

            SSLSocket socket;
            if (succID.getId() != this.peer.getId()) {
                socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(succID.getAddress().getAddress(), succID.getAddress().getPort());
            } else {
                socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(this.peer.getSuccessor().getAddress().getAddress(), this.peer.getSuccessor().getAddress().getPort());
            }
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(msgToSend);



            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


//        this.peer.getChunksToRestore().put(hash, IntStream.range(0, file.getChunkAmount()).boxed().collect(Collectors.toList()));

//        for (int i = 0; i < file.getChunkAmount(); i++) {
//            List<String> msgArgs = new ArrayList(Arrays.asList(this.peer.getProtocolVersion(),
//                    String.valueOf(this.peer.getId()),
//                    hash,
//                    String.valueOf(i)));
//
//            try {
//                Message msgToSend;
//                msgToSend = new Message(MessageType.GETFILE, msgArgs.toArray(new String[0]), null);
//                System.out.println("SENDING GETFILE " + Arrays.toString(msgArgs.toArray(new String[0])));
////                this.peer.getMC().send(msgToSend);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }
    }
}
