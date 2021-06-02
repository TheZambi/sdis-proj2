package g23;

import g23.SSLEngine.SSLEngineOrchestrator;
import g23.SSLEngine.SSLServer;

import javax.net.ssl.SSLContext;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ConnectionDispatcher implements Runnable {

    private final Peer peer;
    private SSLContext sslContext;
    private ServerSocketChannel channel;

    public ConnectionDispatcher(Peer peer) {
        this.peer = peer;
        try {
            this.sslContext = SSLEngineOrchestrator.createContext(false,"123456");
            this.channel = ServerSocketChannel.open();
            this.channel.bind(this.peer.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                SocketChannel socketChannel = this.channel.accept();
                System.out.println("ACCEPTED CONNECTION - NEW SOCKETCHANNEL " + socketChannel.getRemoteAddress());
                this.peer.getProtocolPool().execute(new MessageInterpreter(this.peer, new SSLServer(socketChannel, this.peer.getAddress(), this.sslContext)));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
