package g23.SSLEngine;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class SSLServer extends SSLEngineOrchestrator {

    public SSLServer(SocketChannel socketChannel, InetSocketAddress address, SSLContext sslContext) {
        super(socketChannel, sslContext, address, false);
    }
}
