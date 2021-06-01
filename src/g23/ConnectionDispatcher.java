package g23;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;

public class ConnectionDispatcher implements Runnable {

    private SSLServerSocket serverSocket;
    private final Peer peer;

    public ConnectionDispatcher(Peer peer) {
        this.peer = peer;
        try {
            this.serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(peer.getAddress().getPort(), 5, peer.getAddress().getAddress());

            //NOT SURE TODO
            this.serverSocket.setEnabledCipherSuites(this.serverSocket.getSupportedCipherSuites());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            SSLSocket socket;
            try {
                System.out.println("Starting to accept connection");
                System.out.println(serverSocket);
                socket = (SSLSocket) this.serverSocket.accept();
                System.out.println("Accepted");

                this.peer.getProtocolPool().execute(new MessageInterpreter(this.peer, socket));

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
