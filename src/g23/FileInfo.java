package g23;

import java.io.Serializable;

public class FileInfo implements Serializable, Comparable<FileInfo> {

    private final String path;
    private final long hash;
    private final int desiredReplicationDegree;
    private transient PeerInfo peerInfo;
    private long size;

    public FileInfo(String path, long hash, int desiredReplicationDegree, PeerInfo peerInfo) {
        this.path = path;
        this.hash = hash;
        this.desiredReplicationDegree = desiredReplicationDegree;
        this.peerInfo = peerInfo;
        this.size = -1;
    }

    public FileInfo(String path, long hash, int desiredReplicationDegree, PeerInfo peerInfo, long size) {
        this(path, hash, desiredReplicationDegree, peerInfo);
        this.size = size;
    }

    public long getSize() {
        return this.size;
    }

    public String getPath() {
        return path;
    }

    public long getHash() {
        return hash;
    }

    public int getDesiredReplicationDegree() {
        return desiredReplicationDegree;
    }

    public PeerInfo getPeerInfo() {
        return peerInfo;
    }

    public void setPeerInfo(PeerInfo peerInfo) {
        this.peerInfo = peerInfo;
    }

    @Override
    public int compareTo(FileInfo o) {
        try {
            return Math.toIntExact(this.size - o.getSize());
        } catch (Exception ignored) {
            return 0;
        }
    }
}
