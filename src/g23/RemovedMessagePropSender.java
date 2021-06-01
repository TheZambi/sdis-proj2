package g23;

import g23.Messages.Message;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;

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
                socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(this.peer.getSuccessors().get(i).getAddress().getAddress(), this.peer.getSuccessors().get(i).getAddress().getPort());
                socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(message);

                break;
            } catch(IOException e){
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

