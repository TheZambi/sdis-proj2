package g23;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class PeerInfo implements Serializable {
    private InetSocketAddress address;
    private long id;

    public PeerInfo(InetSocketAddress address, long id) {
        this.address = address;
        this.id = id;
    }

    public InetSocketAddress getAddress(){
        return this.address;
    }

    public long getId(){
        return this.id;
    }
}
