package g23;

import g23.Protocols.Backup.Backup;
import g23.Protocols.Restore.Restore;
import g23.Protocols.Reclaim.Reclaim;
import g23.Protocols.Delete.Delete;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
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

    //Chord can hold 2^m nodes
    //Chord related fields
    private static final int m = 64;

    //Time intervals for stabilization methods
    private static final int STABILIZER_INTERVAL = 500;
    private static final int SUCCESSORS_FINDER_INTERVAL = 500;
    private static final int FINGER_FIXER_INTERVAL = 20;
    private static final int PREDECESSOR_CHECKER_INTERVAL = 500;

    //Chord related services (used to stabilize the network)
    private final ScheduledExecutorService stabilizer;
    private final ScheduledExecutorService successorFinder;
    private final ScheduledExecutorService fingerFixer;
    private final ScheduledExecutorService predecessorChecker;
    private final ScheduledExecutorService protocolPool;
    private final ScheduledExecutorService stateSaver;
    private final ScheduledExecutorService checkIfPeersAreAlive;


    // Chord related information stored by each node
    private List<PeerInfo> fingerTable;
    private PeerInfo predecessor;
    private ArrayList<PeerInfo> successors;
    private int next; //Used for fix_fingers method
    private ConcurrentMap<Long, HashSet<Long>> filesStoredinPeers;

    private final PeerInfo info;

    //Protocol related data structures
    private ConcurrentMap<Long, FileInfo> storedFiles; //Files this peer is currently storing
    private ConcurrentMap<Long, FileInfo> files; // File backups this peer started(FileHash -> FileInfo)
    private Set<Long> filesToRestore; // Files this peer requested to be restored

    //Thread that listens to TCP messages from other peers and launches threads to deal with each one
    private ExecutorService listenerThread;

    //Peer storage
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
            if (!peer.join(new PeerInfo(toJoinInfo, Peer.calculateID(toJoinInfo)))) {
                System.exit(-1);
            }
        }
    }

    public Peer(InetSocketAddress address) throws IOException {

        this.info = new PeerInfo(address, Peer.calculateID(address));
        this.next = 0;

        this.fingerTable = new ArrayList<>();

        for (int i = 0; i < m; i++)
            fingerTable.add(null);

        fingerTable.set(0, this.info);

        this.predecessor = null;
        this.successors = new ArrayList<>();
        this.fingerTable.set(0, this.info);

        this.stabilizer = Executors.newSingleThreadScheduledExecutor();
        this.successorFinder = Executors.newSingleThreadScheduledExecutor();
        this.fingerFixer = Executors.newSingleThreadScheduledExecutor();
        this.predecessorChecker = Executors.newSingleThreadScheduledExecutor();
        this.listenerThread = Executors.newSingleThreadExecutor();
        this.protocolPool = Executors.newScheduledThreadPool(16);
        this.stateSaver = Executors.newSingleThreadScheduledExecutor();
        this.checkIfPeersAreAlive = Executors.newSingleThreadScheduledExecutor();

        this.files = new ConcurrentHashMap<>();
        this.storedFiles = new ConcurrentHashMap<>();
        this.filesStoredinPeers = new ConcurrentHashMap<>();
        this.filesToRestore = new HashSet<>();

        this.bindRMI(this.info.getId());
        this.readState();

        this.listenerThread.execute(new ConnectionDispatcher(this));
        this.stabilizer.scheduleAtFixedRate(this::stabilize, Peer.STABILIZER_INTERVAL + 400, Peer.STABILIZER_INTERVAL, TimeUnit.MILLISECONDS);
        this.successorFinder.scheduleAtFixedRate(this::successorsFinder, Peer.SUCCESSORS_FINDER_INTERVAL + 200, Peer.SUCCESSORS_FINDER_INTERVAL, TimeUnit.MILLISECONDS);
        this.fingerFixer.scheduleAtFixedRate(this::fix_fingers, Peer.FINGER_FIXER_INTERVAL, Peer.FINGER_FIXER_INTERVAL, TimeUnit.MILLISECONDS);
        this.predecessorChecker.scheduleAtFixedRate(this::check_predecessor, Peer.PREDECESSOR_CHECKER_INTERVAL, Peer.PREDECESSOR_CHECKER_INTERVAL, TimeUnit.MILLISECONDS);
        this.stateSaver.scheduleAtFixedRate(new Synchronizer(this), 1, 1, TimeUnit.SECONDS);
        this.checkIfPeersAreAlive.scheduleAtFixedRate(this::peersAreAlive, 5, 5, TimeUnit.SECONDS);

        this.printInfo();
    }


    public void peersAreAlive() {
        ArrayList<Map.Entry<Long, Long>> stuffToDelete = new ArrayList<>();
        try {
            for (Map.Entry<Long, HashSet<Long>> entry : this.filesStoredinPeers.entrySet()) {
                for (Long l : entry.getValue()) {

                    if (this.findSuccessor(l).getId() != l) {
                        stuffToDelete.add(new AbstractMap.SimpleEntry<>(entry.getKey(), l));

                        this.protocolPool.schedule(new Backup(this,
                                        this.files.get(entry.getKey()).getPath(), 1, 1),
                                5, TimeUnit.SECONDS);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Peer died.");
        }

        for (Map.Entry<Long, Long> toDelete : stuffToDelete) {
            this.filesStoredinPeers.get(toDelete.getKey()).remove(toDelete.getValue());
        }
    }

    private void readState() {
        try (FileInputStream stateIn = new FileInputStream("peerState");
             ObjectInputStream stateInObject = new ObjectInputStream(stateIn)) {
            PeerState peerState = (PeerState) stateInObject.readObject();
            currentSpace = peerState.currentSpace;
            maxSpace = peerState.maxSpace;
            storedFiles = (ConcurrentMap<Long, FileInfo>) peerState.storedfiles;
            files = (ConcurrentMap<Long, FileInfo>) peerState.files;
        } catch (FileNotFoundException ignored) {
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void backup(String path, int replicationDegree) throws RemoteException {
        this.protocolPool.execute(new Backup(this, path, replicationDegree, replicationDegree));
    }

    @Override
    public void restore(String path) throws RemoteException {
        this.protocolPool.execute(new Restore(this, path));
    }

    @Override
    public void delete(String path) throws RemoteException {
        this.protocolPool.execute(new Delete(this, path));
    }

    @Override
    public void reclaim(long amountOfKBytes) throws RemoteException {
        this.protocolPool.execute(new Reclaim(this, amountOfKBytes * 1000));
    }

    @Override
    public PeerState state() throws RemoteException {
        return new PeerState(maxSpace, currentSpace, files, storedFiles, null); //TODO CHANGE ONGOING
    }

    public void printInfo() {
        System.out.println("---------INFO--------");
        System.out.println("ID: " + this.getId());
        System.out.println("IP address: " + this.getAddress().getAddress() + ":" + this.getAddress().getPort());
        System.out.println("----------------------");
    }

    public void bindRMI(long id) throws RemoteException {
        ChordNode stub = (ChordNode) UnicastRemoteObject.exportObject(this, 0);

        try {
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(String.valueOf(id), stub);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Couldn't bind RMI");
        }
    }

    private static long calculateID(InetSocketAddress address) {

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] hash = digest.digest(address.toString().getBytes());
        UUID id = UUID.nameUUIDFromBytes(hash);
        long idInt = id.getLeastSignificantBits() * -1; // UUID LSB always returns negative numbers
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
                for (int i = 0; i < this.fingerTable.size(); i++) {
                    PeerInfo pi = this.fingerTable.get(i);
                    if (pi.getId() == closestPeer.getId())
                        this.fingerTable.set(i, null);

                }
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

    public boolean chordIdInBetween(long id, PeerInfo peer1, PeerInfo peer2) {
        try {
            if (peer1.getId() < peer2.getId()) {
                return peer1.getId() < id && id <= peer2.getId();
            } else {
                return (peer1.getId() < id && id < Math.pow(2, m)) || (0 <= id && id <= peer2.getId());
            }
        }catch (Exception e)
        {
            return false;
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
        PeerInfo ni = null;
        for (int i = 0; i < this.successors.size(); i++) {
            try {
                Registry registry = LocateRegistry.getRegistry();
                ChordNode stub = (ChordNode) registry.lookup(String.valueOf(successors.get(i).getId()));
                ni = stub.getPredecessor();
                break;
            } catch (Exception e) {
                if (i == 0)
                    this.fingerTable.set(0, this.info);
                if (i == this.successors.size() - 1) {
                    System.out.println("Couldn't find any active successor, exiting stabilization.");
                    return;
                }
                System.out.println("Couldn't find successor to stabilize, trying the next on the successor list.");
            }
        }


        if (ni != null && chordIdInBetween(ni.getId(), this.info, this.fingerTable.get(0))) {
            fingerTable.set(0, ni);
        }

        if (ni != null && (this.fingerTable.get(0).getId() == this.getId()))
            fingerTable.set(0, ni);

        this.sendNotification(this.fingerTable.get(0));
    }

    public void successorsFinder() {
        PeerInfo ni = null;
        try {
            Registry registry = LocateRegistry.getRegistry();
            ChordNode stub = (ChordNode) registry.lookup(String.valueOf(fingerTable.get(0).getId()));
            ni = stub.getSuccessor();
        } catch (Exception e) {
        }


        PeerInfo ns = null;
        try {
            if (ni != null)
                if (ni.getId() != this.getId()) {
                    Registry registry = LocateRegistry.getRegistry();
                    ChordNode stub = (ChordNode) registry.lookup(String.valueOf(ni.getId()));
                    ns = stub.getSuccessor();
                }
        } catch (Exception e) {
        }

        this.getSuccessors().clear();
        this.getSuccessors().add(fingerTable.get(0));
        if (ni != null)
            if (ni.getId() != this.getId() && !this.getSuccessors().contains(ni))
                this.getSuccessors().add(ni);
        if (ns != null)
            if (ns.getId() != this.getId() && !this.getSuccessors().contains(ns))
                this.getSuccessors().add(ns);

    }

    // TODO fail to join
    public boolean join(PeerInfo peer) {
        predecessor = null;

        Registry registry;
        ChordNode stub;
        try {
            registry = LocateRegistry.getRegistry();
            stub = (ChordNode) registry.lookup(String.valueOf(peer.getId()));
            PeerInfo response = stub.findSuccessor(this.getId());

            fingerTable.set(0, response);

        } catch (RemoteException | NotBoundException e) {
            System.err.println("FAILED TO JOIN CHORD. EXITING GRACEFULLY...");
            return false;
        }

        return true;
    }

    private void sendNotification(PeerInfo node) {
        if (node.getId() == this.getId())
            return;
        Registry registry;
        ChordNode stub;
        try {
            registry = LocateRegistry.getRegistry();
            stub = (ChordNode) registry.lookup(String.valueOf(node.getId()));
            boolean response = stub.notify(this.info);

        } catch (RemoteException | NotBoundException e) {
        }

    }

    public boolean notify(PeerInfo node) throws RemoteException {
        if (this.predecessor == null || chordIdInBetween(node.getId(), this.predecessor, this.info)) {
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
        try {
            fingerTable.set(next - 1, findSuccessor((this.getId() + (long) Math.pow(2, next - 1)) % (long) Math.pow(2, m)));
        } catch (RemoteException e) {
            this.fingerTable.set(next - 1, null);
        }

    }

    public boolean isAlive() {
        return true;
    }

    public boolean check_predecessor() {
        if (this.predecessor != null) {
            Registry registry;
            ChordNode stub;

            try {
                registry = LocateRegistry.getRegistry();
                stub = (ChordNode) registry.lookup(String.valueOf(this.predecessor.getId()));
                return stub.isAlive();

            } catch (RemoteException | NotBoundException e) {

                this.predecessor = null;

                return false;
            }

        }

        return false;
    }

    public PeerInfo getPeerInfo() {
        return this.info;
    }

    public List<PeerInfo> getFingerTable() {
        return this.fingerTable;
    }

    public PeerInfo getPredecessor() {
        return this.predecessor;
    }

    public PeerInfo getSuccessor() {
        return this.fingerTable.get(0);
    }

    public ArrayList<PeerInfo> getSuccessors() {
        return successors;
    }

    public void setSuccessors(ArrayList<PeerInfo> successors) {
        this.successors = successors;
    }

    public long getId() {
        return info.getId();
    }

    public InetSocketAddress getAddress() {
        return info.getAddress();
    }

    public ScheduledExecutorService getProtocolPool() {
        return protocolPool;
    }

    public ConcurrentMap<Long, FileInfo> getFiles() {
        return files;
    }

    public ConcurrentMap<Long, FileInfo> getStoredFiles() {
        return storedFiles;
    }

    public ConcurrentMap<Long, HashSet<Long>> getFilesStoredInPeers() {
        return filesStoredinPeers;
    }

    public long getRemainingSpace() {
        return maxSpace - currentSpace;
    }

    public void addSpace(long length) {
        this.currentSpace += length;
    }

    public void removeSpace(long space) {
        currentSpace -= space;
    }

    public long getCurrentSpace() {
        return currentSpace;
    }

    public Set<Long> getFilesToRestore() {
        return filesToRestore;
    }

    public static long getFileId(String path, long peerID) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest((path + Files.getLastModifiedTime(Path.of(path)) + peerID).getBytes());

            UUID id = UUID.nameUUIDFromBytes(hash);
            long idLong = id.getLeastSignificantBits() * -1;
            idLong = idLong % (long) Math.pow(2, Peer.m);

            return idLong;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }

    public void setMaxSpace(long maxSpace) {
        this.maxSpace = maxSpace;
    }
}
