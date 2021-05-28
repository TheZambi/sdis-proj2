package g23;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class PeerState implements Serializable {

    long maxSpace; // bytes
    long currentSpace; // bytes

    Map<String, FileInfo> files; // FileHash -> FileInfo
    Map<String, FileInfo> storedfiles; // FileHash -> FileInfo
    Map<String, Set<Integer>> peersDidNotDeleteFiles;
    Set<String> onGoingOperations;


    public PeerState(long maxSpace, long currentSpace, Map<String, FileInfo> files, Map<String, FileInfo> storedFiles, Map<String, Set<Integer>> deleted, Set<String> ongoing) {
        this.maxSpace = maxSpace;
        this.currentSpace = currentSpace;
        this.files = files;
        this.storedfiles = storedFiles;
        this.peersDidNotDeleteFiles = deleted;
        this.onGoingOperations = ongoing;
    }

    @Override
    public String toString() {
        String result = "";
        result += "-------------INITIATED BACKUPS-------------\n";
        for (Map.Entry<String, FileInfo> entry : files.entrySet()){
            result += "\t-------------FILE---------------\n";
            result += "Name: " + entry.getValue().getPath() + "\n";
            result += "File ID: " + entry.getKey() + "\n";
            result += "Desired Replication Degree: " + entry.getValue().getDesiredReplicationDegree() + "\n";
        }

        result += "---------------STORED FILES---------------\n";
        for (Map.Entry<String, FileInfo> entry : storedfiles.entrySet()){
            result += "-------------Chunk---------------\n";
            result += "File ID: " + entry.getValue().getHash() + "\n";
            result += "Size: " + entry.getValue().getSize() / 1000 + "KB\n";
            result += "Desired Replication Degree: " + entry.getValue().getDesiredReplicationDegree() + "\n";
        }

        result += "------------------STORAGE------------------\n";
        result += "Total Capacity: " + this.maxSpace / 1000 + "KB\n";
        result += "Current used space: " + this.currentSpace / 1000 + "KB\n";

//        result += "---------------Peer to Delete---------------\n";
//        for (Map.Entry<String, Set<Integer>> entry : peersDidNotDeleteFiles.entrySet()){
//            result += "-------------File---------------\n";
//            result += "File Hash: " + entry.getKey() + "\n";
//            result += "Peers left to delete: ";
//            for(Integer peer : entry.getValue()) {
//                result += peer + ", ";
//            }
//            result += "\n";
//        }

        return result;
    }
}
