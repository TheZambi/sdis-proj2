package g23.SSLEngine;

import g23.MessageInterpreter;
import g23.Peer;
import g23.SSLEngine.SSLEngineOrchestrator;
import g23.SSLEngine.SSLServer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.nio.channels.Channels;
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

                this.peer.getProtocolPool().execute(new MessageInterpreter(this.peer, new SSLServer(socketChannel, this.peer.getAddress(), this.sslContext)));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

//    public ConnectionDispatcher(Peer peer) {
//        this.peer = peer;
//        try {
//            this.serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(peer.getAddress().getPort(), 5, peer.getAddress().getAddress());
//
//            //NOT SURE TODO
//            this.serverSocket.setEnabledCipherSuites(this.serverSocket.getSupportedCipherSuites());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void run() {
//        while (true) {
//            SSLSocket socket;
//            try {
//                System.out.println("Starting to accept connection");
//                System.out.println(serverSocket);
//                socket = (SSLSocket) this.serverSocket.accept();
//                System.out.println("Accepted");
//
//                this.peer.getProtocolPool().execute(new MessageInterpreter(this.peer, socket));
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
//    }
}
