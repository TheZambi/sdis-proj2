package g23;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class PeerState implements Serializable {

    long maxSpace; // bytes
    long currentSpace; // bytes

    Map<Long, FileInfo> files; // FileHash -> FileInfo
    Map<Long, FileInfo> storedfiles; // FileHash -> FileInfo
    Set<String> onGoingOperations;

    public PeerState(long maxSpace, long currentSpace, Map<Long, FileInfo> files, Map<Long, FileInfo> storedFiles, Set<String> ongoing) {
        this.maxSpace = maxSpace;
        this.currentSpace = currentSpace;
        this.files = files;
        this.storedfiles = storedFiles;
        this.onGoingOperations = ongoing;
    }

    @Override
    public String toString() {
        String result = "";
        result += "-------------INITIATED BACKUPS-------------\n";
        for (Map.Entry<Long, FileInfo> entry : files.entrySet()) {
            result += "\t-------------FILE---------------\n";
            result += "Name: " + entry.getValue().getPath() + "\n";
            result += "File ID: " + entry.getKey() + "\n";
            result += "Desired Replication Degree: " + entry.getValue().getDesiredReplicationDegree() + "\n";
        }

        result += "---------------STORED FILES---------------\n";
        for (Map.Entry<Long, FileInfo> entry : storedfiles.entrySet()) {
            result += "-------------Chunk---------------\n";
            result += "File ID: " + entry.getValue().getHash() + "\n";
            result += "Size: " + entry.getValue().getSize() / 1000 + "KB\n";
            result += "Desired Replication Degree: " + entry.getValue().getDesiredReplicationDegree() + "\n";
        }

        result += "------------------STORAGE------------------\n";
        result += "Total Capacity: " + this.maxSpace / 1000 + "KB\n";
        result += "Current used space: " + this.currentSpace / 1000 + "KB\n";

        return result;
    }
}
