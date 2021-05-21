package g23;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionDispatcher implements Runnable {

    private ServerSocket serverSocket;
    private Peer peer;

    public ConnectionDispatcher(Peer peer) {
        this.peer = peer;
        try {
            this.serverSocket = new ServerSocket(peer.getAddress().getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            Socket socket = null;
            try {
                socket = this.serverSocket.accept();
                System.out.println("Accepted");

                this.peer.getProtocolPool().execute(new MessageInterpreter(this.peer, socket));

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}