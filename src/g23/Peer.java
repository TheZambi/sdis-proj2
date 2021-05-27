package g23;

import g23.Chunk;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

public class Peer implements ChordNode {

    //    private static final int m = 256;
    private static final int m = 5;
    private static final int STABILIZER_INTERVAL = 5;
    private static final int FINGER_FIXER_INTERVAL = 1;
    private static final int PREDECESSOR_CHECKER_INTERVAL = 5;

    private PeerInfo info;

    private List<PeerInfo> fingerTable;
    private HashMap<String, String> data; //TODO

    private PeerInfo predecessor;
//    private PeerInfo successor;

    private int next; //Used for fix_fingers method

    private ConnectionDispatcher connectionDispatcher;

    ConcurrentMap<String, FileInfo> storedFiles;
    ConcurrentMap<String, FileInfo> files; // FileHash -> FileInfo
    ConcurrentMap<String, ScheduledFuture<?>> messagesToSend;
    ConcurrentMap<String, ScheduledFuture<?>> backupsToSend; //FOR THE RECLAIM PROTOCOL
    ConcurrentMap<String, List<Integer>> chunksToRestore;
    private ScheduledExecutorService stabilizer;

    private ScheduledExecutorService fingerFixer;
    private ScheduledExecutorService predecessorChecker;
    private ScheduledExecutorService protocolPool;

    long maxSpace = 100000000000L; // bytes
    long currentSpace = 0; // bytes

    public static void main(String[] args) throws IOException {

        String address = args[0].split(":")[0];
        int port = Integer.parseInt(args[0].split(":")[1]);

        Peer peer = new Peer(new InetSocketAddress(address, port));
        if (args.length == 2) {
            String toJoinAddress = args[1].split(":")[0];
            int toJoinPort = Integer.parseInt(args[1].split(":")[1]);

            InetSocketAddress toJoinInfo = new InetSocketAddress(toJoinAddress, toJoinPort);
            peer.join(new PeerInfo(toJoinInfo, Peer.calculateID(toJoinInfo)));
        }
    }

    public Peer(InetSocketAddress address) throws IOException {

        this.info = new PeerInfo(address, Peer.calculateID(address));
        this.next = 0;

        this.fingerTable = new ArrayList<>();


        for (int i = 0; i < m; i++)
            fingerTable.add(null);

        fingerTable.set(0, this.info);

        this.connectionDispatcher = new ConnectionDispatcher(this);

        this.predecessor = null;
        this.fingerTable.set(0, new PeerInfo(address, Peer.calculateID(address)));

        this.stabilizer = Executors.newSingleThreadScheduledExecutor();
        this.fingerFixer = Executors.newSingleThreadScheduledExecutor();
        this.predecessorChecker = Executors.newSingleThreadScheduledExecutor();
        this.protocolPool = Executors.newScheduledThreadPool(16);

        this.files = new ConcurrentHashMap<>();

        this.bindRMI(this.info.getId());

        this.stabilizer.scheduleAtFixedRate(this::stabilize, Peer.STABILIZER_INTERVAL, Peer.STABILIZER_INTERVAL, TimeUnit.SECONDS);
        this.fingerFixer.scheduleAtFixedRate(this::fix_fingers, Peer.FINGER_FIXER_INTERVAL * 5, Peer.FINGER_FIXER_INTERVAL, TimeUnit.SECONDS);
        this.predecessorChecker.scheduleAtFixedRate(this::check_predecessor, Peer.PREDECESSOR_CHECKER_INTERVAL, Peer.PREDECESSOR_CHECKER_INTERVAL, TimeUnit.SECONDS);

        this.printInfo();
    }

    public void printInfo() {
        System.out.println("---------INFO--------");
        System.out.println("ID: " + this.getId());
        System.out.println("IP address: " + this.getAddress().getAddress() + ":" + this.getAddress().getPort());
        System.out.println("Predecessor: " + this.predecessor);
        for (int i = 0; i < Peer.m; i++) {
            System.out.println("Finger " + i + ":" + fingerTable.get(i));
        }
        System.out.println("----------------------");
    }

    public void bindRMI(long id) throws RemoteException {
        ChordNode stub = (ChordNode) UnicastRemoteObject.exportObject(this, 0);

        try {
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(String.valueOf(id), stub);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Couldn't bind RMI");
        }
    }

    private static long calculateID(InetSocketAddress address) {

//        MessageDigest digest = null;
//        try {
//            digest = MessageDigest.getInstance("SHA-256");
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//
//        byte[] hash = digest.digest(address.toString().getBytes());
//        UUID id = UUID.nameUUIDFromBytes(hash);
//        long idInt = id.getLeastSignificantBits() * -1;
////        idInt = Math.round(idInt % Math.pow(2, Peer.m));

        long idInt = address.getPort() - 5000;
        idInt = Math.round(idInt % Math.pow(2, Peer.m));
        return idInt;
    }

    public PeerInfo findSuccessor(long id) throws RemoteException {

        if (chordIdInBetween(id, this.info, this.fingerTable.get(0))) {
            return fingerTable.get(0);
        } else {
            PeerInfo closestPeer = this.closestPrecedingPeer(id);
            if (closestPeer.getId() == this.info.getId()) {
                return this.info;
            }

            try {
                Registry registry = LocateRegistry.getRegistry();
                ChordNode stub = (ChordNode) registry.lookup(String.valueOf(closestPeer.getId()));

                return stub.findSuccessor(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private PeerInfo closestPrecedingPeer(long id) {
        for (int i = fingerTable.size() - 1; i >= 0; i--) {
            if (fingerTable.get(i) != null && chordIdInBetween(fingerTable.get(i).getId(), this.info.getId(), id)) {
                return fingerTable.get(i);
            }
        }
        return this.info;
    }

    public PeerInfo getPredecessor() {
        return this.predecessor;
    }

    @Override
    public void backup(String path, int replicationDegree) throws RemoteException {

    }

    @Override
    public void restore(String path) throws RemoteException {

    }

    @Override
    public void delete(String path) throws RemoteException {

    }

    @Override
    public void reclaim(long amountOfBytes) throws RemoteException {

    }

    @Override
    public PeerState state() throws RemoteException {
        return null;
    }

    public boolean chordIdInBetween(long id, PeerInfo peer1, PeerInfo peer2) {
        if (peer1.getId() < peer2.getId()) {
            return peer1.getId() < id && id < peer2.getId();
        } else {
            return (peer1.getId() < id && id < Math.pow(2, m)) || (0 <= id && id < peer2.getId());
        }
    }

    public boolean chordIdInBetween(long id, long peer1, long peer2) {
        if (peer1 < peer2) {
            return peer1 < id && id < peer2;
        } else {
            return (peer1 < id && id < Math.pow(2, m)) || (0 <= id && id < peer2);
        }
    }

    public void stabilize() {
        System.out.println("Starting Stabilization");
        PeerInfo ni = null;
        try {
            Registry registry = LocateRegistry.getRegistry();
            ChordNode stub = (ChordNode) registry.lookup(String.valueOf(fingerTable.get(0).getId()));
            ni = stub.getPredecessor();
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (ni != null && chordIdInBetween(ni.getId(), this.info, this.fingerTable.get(0))) {
            fingerTable.set(0, ni);
        }

        if (ni != null && (this.fingerTable.get(0).getId() == this.getId()))
            fingerTable.set(0, ni);

        this.sendNotification(this.fingerTable.get(0));
        System.out.println("Ending Stabilization");
        printInfo();
    }

    public void join(PeerInfo peer) {
        predecessor = null;

        Registry registry;
        ChordNode stub;
        try {
            registry = LocateRegistry.getRegistry();
            stub = (ChordNode) registry.lookup(String.valueOf(peer.getId()));
            PeerInfo response = stub.findSuccessor(this.getId());
            System.out.println("XXXXXXXXXXXXXXXXXXXXXXXX");

            fingerTable.set(0, response);

            System.out.println("ZZZZZZZZZZZZZZZZZZZZZZZZ");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

        System.out.println("JOINED - SUCCESSOR: " + this.fingerTable.get(0).getId());
        this.printInfo();
    }


    private void sendNotification(PeerInfo node) {
        if (node.getId() == this.getId())
            return;
        Registry registry;
        ChordNode stub;
        try {
            registry = LocateRegistry.getRegistry();
            stub = (ChordNode) registry.lookup(String.valueOf(node.getId()));
            System.out.println("Got Sending notification to: " + node);
            boolean response = stub.notify(this.info);

        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

    }

    public boolean notify(PeerInfo node) throws RemoteException {
        System.out.println("Got notified by: " + node);
        if (this.predecessor == null || chordIdInBetween(node.getId(), this.predecessor, this.info)) {
            System.out.println("My new predecessor is: " + node);
            this.predecessor = node;
            return true;
        }
        return false;
    }

    private void fix_fingers() {

        next += 1;
        if (next > m) {
            next = 1;
        }
        System.out.println("Starting Fix Finger" + (next - 1));
        try {
            fingerTable.set(next - 1, findSuccessor((this.getId() + (int) Math.pow(2, next - 1)) % (int) Math.pow(2, m)));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        System.out.println("Ending Fix Finger" + (next - 1));
        printInfo();

    }

    public boolean isAlive() {
        return true;
    }

    public boolean check_predecessor() {
        System.out.println("Starting Check For Predecessors");
        if (this.predecessor != null) {
            Registry registry;
            ChordNode stub;

            try {
                registry = LocateRegistry.getRegistry();
                stub = (ChordNode) registry.lookup(String.valueOf(this.predecessor.getId()));
                System.out.println("Ending Check For Predecessors : Return True");
                return stub.isAlive();

            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
                System.out.println("Ending Check For Predecessors : Return False");

                return false;
            }

        }
        System.out.println("Ending Check For Predecessors : Return False");

        return false;
    }

    public long getId() {
        return info.getId();
    }

    public InetSocketAddress getAddress() {
        return info.getAddress();
    }

//    public void communicate() throws IOException {
//        while (true) {
//            Socket socket = this.serverSocket.accept();
//
//            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
//            System.out.println("Accepted");
//
//            byte[] readData = new byte[64000];
//            int nRead = in.read(readData, 0, 64000);
//
//            byte[] aux = Arrays.copyOfRange(readData, 0, nRead);
//
//            System.out.println(nRead);
//
//            String[] data = (new String(aux)).split(" ");
//
//            System.out.println(data[1].split("\0")[0]);
//        }
//
//    }

    public String getProtocolVersion() {
        return "1.0";
    }

    public ScheduledExecutorService getProtocolPool() {
        return protocolPool;
    }

    public ConcurrentMap<String, FileInfo> getFiles() {
        return files;
    }

    public ConcurrentMap<String, FileInfo> getStoredFiles() {
        return storedFiles;
    }

    public long getRemainingSpace(){
        return maxSpace - currentSpace;
    }

    public void addSpace(int length) {
        this.currentSpace += length;
    }

    public ConcurrentMap<String, ScheduledFuture<?>> getBackupsToSend() {
        return backupsToSend;
    }

    public ConcurrentMap<String, ScheduledFuture<?>> getMessagesToSend() {
        return messagesToSend;
    }

    public long getCurrentSpace(){
        return currentSpace;
    }

    public static long getFileId(String path, long peerID) {
//        MessageDigest digest = null;
//
//        File file = new File(path);
//
//        try {
//            digest = MessageDigest.getInstance("SHA-256");
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//        byte[] hash = digest.digest((path + file.lastModified() + peerID).getBytes());
//
//        StringBuilder result = new StringBuilder();
//        for (byte b : hash) {
//            result.append(Character.forDigit((b >> 4) & 0xF, 16))
//                    .append(Character.forDigit((b & 0xF), 16));
//        }


//        return result.toString();

        return 5;
    }
}
