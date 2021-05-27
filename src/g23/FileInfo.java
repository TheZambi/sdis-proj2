package g23;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FileInfo implements Serializable {

    private final String path;
    private final String hash;
    private final int desiredReplicationDegree;
    private ChordNode peerInfo;

    public FileInfo(String path, String hash, int desiredReplicationDegree, ChordNode peerInfo) {
        this.path = path;
        this.hash = hash;
        this.desiredReplicationDegree = desiredReplicationDegree;
        this.peerInfo = peerInfo;
    }

    public String getPath() {
        return path;
    }

    public String getHash() {
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
