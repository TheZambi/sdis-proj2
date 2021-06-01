package g23.SSLEngine;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SSLClient extends SSLEngineOrchestrator {

    private SocketChannel socketChannel;
    private SSLContext sslContext;
    private InetSocketAddress address;

    public SSLClient(InetSocketAddress address) throws Exception {
        super(SocketChannel.open(), SSLEngineOrchestrator.createContext(true,"123456"), address, true);
        this.address = address;
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
