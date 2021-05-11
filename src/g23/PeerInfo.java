package g23;

import java.net.InetSocketAddress;

public class PeerInfo {
    private InetSocketAddress address;
    private Integer id;

    public PeerInfo(InetSocketAddress address, Integer id) {
        this.address = address;
        this.id = id;
    }

    public InetSocketAddress getAddress(){
        return this.address;
    }

    public Integer getId(){
        return this.id;
    }
}
