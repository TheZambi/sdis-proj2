package g23;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class PeerInfo implements Serializable {
    private final InetSocketAddress address;
    private final long id;

    public PeerInfo(InetSocketAddress address, long id) {
        this.address = address;
        this.id = id;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public long getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "PeerInfo{" +
                "address=" + address.getAddress() + ":" + address.getPort() +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof PeerInfo))
            return false;

        return this.address.equals(((PeerInfo) obj).getAddress()) && this.id == ((PeerInfo) obj).getId();
    }
}
