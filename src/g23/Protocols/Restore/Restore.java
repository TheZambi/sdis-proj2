package g23.Protocols.Restore;

import g23.FileInfo;
import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Peer;
import g23.PeerInfo;
import g23.SSLEngine.SSLClient;

import java.io.ByteArrayOutputStream;
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

            SSLClient sslClient = null;

            if (succID.getId() != this.peer.getId()) {
                sslClient = new SSLClient(succID.getAddress());
            } else {

                for (int i = 0; i < this.peer.getSuccessors().size(); i++) {
                    try {
                        sslClient = new SSLClient(this.peer.getSuccessor().getAddress());
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

            if (sslClient == null) {
                return;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(msgToSend);
            oos.flush();
            byte[] msg = bos.toByteArray();
            sslClient.write(msg, msg.length);
            bos.close();
            sslClient.shutdown();

            System.out.println("SENT GETFILE (" + msgToSend.getFileId() + ") TO " + sslClient.getAddress().getAddress() + ":" + sslClient.getAddress().getPort());

            //Add file to set of files to restore so that when we receive a conection it can be validated
            this.peer.getFilesToRestore().add(fileId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
