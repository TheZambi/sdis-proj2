package g23.SSLEngine;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;


public class SSLServer extends SSLEngineOrchestrator {

    private SSLContext sslContext;
    private SocketChannel socketChannel;


    protected SSLServer(SocketChannel socketChannel, InetSocketAddress address, SSLContext sslContext) {
        super(socketChannel, sslContext, address, false);
        this.sslContext = sslContext;
    }


     public void close() throws IOException {
        this.socketChannel.close();
     }
}
