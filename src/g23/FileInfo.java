package g23;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FileInfo implements Serializable {

    private final String path;
    private final long hash;
    private final int desiredReplicationDegree;
    private ChordNode peerInfo;
    private long size;

    public FileInfo(String path, long hash,int currentReplicationDegree, int desiredReplicationDegree, ChordNode peerInfo) {
        this.path = path;
        this.hash = hash;
        this.desiredReplicationDegree = desiredReplicationDegree;
        this.peerInfo = peerInfo;
        this.size = -1;
    }

    public FileInfo(String path, long hash,int currentReplicationDegree, int desiredReplicationDegree, ChordNode peerInfo, long size) {
        this(path, hash, currentReplicationDegree, desiredReplicationDegree, peerInfo);
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



//    public int getChunkAmount() {
//        return chunksPeers.size();
//    }

    public ChordNode getPeerInfo() {
        return peerInfo;
    }

    public void setPeerInfo(ChordNode peerInfo) { this.peerInfo = peerInfo; }

//    public boolean allSent() {
//        for(Chunk chunk : chunksPeers) {
//            if(!chunk.isSent())
//                return false;
//        }
//        return true;
//    }
}
