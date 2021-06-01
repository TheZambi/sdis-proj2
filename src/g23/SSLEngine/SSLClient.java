package g23.SSLEngine;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SSLClient extends SSLEngineOrchestrator {

    private SocketChannel socketChannel;
    private SSLContext sslContext;

    protected SSLClient(InetSocketAddress address, SSLContext context) throws Exception {
        super(SocketChannel.open().bind(address), SSLEngineOrchestrator.createContext(true,"123456"), address, true);
        this.sslContext = context;
    }

}
