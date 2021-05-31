package g23;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;

public class Synchronizer implements Runnable {
    Peer peer;

    public Synchronizer(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        checkFiles();
        writePeerState();
    }

    private void checkFiles() {
        for (FileInfo fileInfo : peer.getFiles().values()) {
            if (!Files.exists(Path.of(fileInfo.getPath())) || Peer.getFileId(fileInfo.getPath(), peer.getId()) != fileInfo.getHash()) {
                try {
                    peer.delete(fileInfo.getPath());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void writePeerState() {
        try (FileOutputStream fileOutFiles = new FileOutputStream("peerState");
             ObjectOutputStream outFiles = new ObjectOutputStream(fileOutFiles)) {
            PeerState peerState = peer.state();
            outFiles.writeObject(peerState);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
