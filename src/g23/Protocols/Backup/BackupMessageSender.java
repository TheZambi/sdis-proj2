package g23.Protocols.Backup;

import g23.Messages.Message;
import g23.Peer;
import g23.PeerInfo;
import g23.SSLEngine.SSLClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashSet;

/* For sending PUTFILE / REMOVED messages */
public class BackupMessageSender implements Runnable {
    private Peer peer;
    private Message message;

    public BackupMessageSender(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void run() {

        try {
            long fileId = message.getFileId();
            PeerInfo succ = this.peer.findSuccessor(fileId);

            SSLClient toSendMsg = null;

            if (succ.getId() != this.peer.getId()) {
                System.out.println(succ.getAddress());
                 toSendMsg = new SSLClient(succ.getAddress());
            } else {
                for (int i = 0; i < this.peer.getSuccessors().size(); i++) {
                    try {
                        toSendMsg = new SSLClient(this.peer.getSuccessors().get(i).getAddress());

                        succ = this.peer.getSuccessors().get(i);
                        break;
                    } catch (IOException e) {
                        if (i == 0) {
                            this.peer.getFingerTable().set(0, this.peer.getPeerInfo());
                        }
                        if (i == this.peer.getSuccessors().size() - 1) {
                            System.out.println("Couldn't connect with any successor to send BACKUP.");
                            return;
                        }
                    }
                }
            }

            if (toSendMsg == null) {
                return;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this.message);
            oos.flush();
            byte[] msg = bos.toByteArray();
            toSendMsg.write(msg, msg.length);
            bos.close();

            System.out.println("SENT " + this.message.getType().toString() + "(" + fileId + ", replication="+ message.getReplicationDegree()
                    + ") TO " + toSendMsg.getAddress().getAddress() + ":" + toSendMsg.getAddress().getPort());

            toSendMsg.shutdown();


            if (!this.peer.getFilesStoredInPeers().containsKey(message.getFileId())) {
                HashSet<Long> set = new HashSet<>();
                set.add(succ.getId());
                this.peer.getFilesStoredInPeers().put(message.getFileId(), set);
            } else {
                this.peer.getFilesStoredInPeers().get(message.getFileId()).add(succ.getId());
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
