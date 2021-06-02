package g23.Protocols.Reclaim;

import g23.Messages.Message;
import g23.Messages.MessageType;
import g23.Peer;
import g23.SSLEngine.SSLClient;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class RemovedMessagePropSender implements Runnable {

    private Peer peer;
    private Message message;

    public RemovedMessagePropSender(Message message, Peer peer) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void run() {
        SSLSocket socket;

        for (int i = 0; i < this.peer.getSuccessors().size(); i++) {
            try {
                SSLClient fromServer = new SSLClient(this.peer.getSuccessors().get(i).getAddress());

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(message);
                oos.flush();
                byte[] msg = bos.toByteArray();
                fromServer.write(msg, msg.length);
                bos.close();

                System.out.println("FORWARDED REMOVE (" + message.getFileId() + ") TO " +
                        this.peer.getSuccessors().get(i).getAddress().getAddress() + ":" + this.peer.getSuccessors().get(i).getAddress().getPort());

                fromServer.shutdown();

                break;
            } catch(Exception e){
                if(i == 0)
                    this.peer.getFingerTable().set(0,this.peer.getPeerInfo());
                if(i == this.peer.getSuccessors().size() - 1)
                {
                    System.out.println("Couldn't connect with any successor to send REMOVED.");
                    return;
                }
            }
        }
    }
}

