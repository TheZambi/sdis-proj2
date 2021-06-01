package g03.Protocols;

import g23.FileInfo;
import g23.Peer;

import java.util.Map;
import java.util.stream.Stream;

public class Restore implements Runnable {
    private final Peer peer;
    private final String path;

    public Restore(Peer peer, String path) {
        this.peer = peer;
        this.path = path;
    }

    @Override
    public void run() {
        Stream<Map.Entry<Long, FileInfo>> matches = this.peer.getFiles().entrySet().stream().filter(f -> f.getValue().getPath().equals(path));

        FileInfo file = matches.findFirst().get().getValue();
        if(file == null) {
            System.err.println("File not found in backup system");
        }
        long hash = file.getHash();

//        this.peer.getChunksToRestore().put(hash, IntStream.range(0, file.getChunkAmount()).boxed().collect(Collectors.toList()));

//        for (int i = 0; i < file.getChunkAmount(); i++) {
//            List<String> msgArgs = new ArrayList(Arrays.asList(this.peer.getProtocolVersion(),
//                    String.valueOf(this.peer.getId()),
//                    hash,
//                    String.valueOf(i)));
//
//            try {
//                Message msgToSend;
//                msgToSend = new Message(MessageType.GETFILE, msgArgs.toArray(new String[0]), null);
//                System.out.println("SENDING GETFILE " + Arrays.toString(msgArgs.toArray(new String[0])));
////                this.peer.getMC().send(msgToSend);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }
    }
}
