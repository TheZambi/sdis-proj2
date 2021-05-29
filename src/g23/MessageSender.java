package g23;

import g23.Messages.Message;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class MessageSender implements Runnable {

    private PeerInfo peer;
    private Message message;
    private InetAddress address;
    private int port;

    public MessageSender(Message message, InetAddress address, int port) {
        this.peer = peer;
        this.message = message;
        this.address = address;
        this.port = port;
    }

    @Override
    public void run() {
        SSLSocket socket;
        try {
            socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(address, port);
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

