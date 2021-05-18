package g23;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private ServerSocket serverSocket;


    private ScheduledExecutorService stabilizer;
    private ScheduledExecutorService fingerFixer;
    private ScheduledExecutorService predecessorChecker;

    public Peer(InetSocketAddress address) throws IOException {

        this.info = new PeerInfo(address, Peer.calculateID(address));
        this.next = 0;

        this.fingerTable = new ArrayList<>();
        for (int i = 0; i < m; i++)
            fingerTable.add(this.info);

        this.serverSocket = new ServerSocket(address.getPort());

        this.predecessor = null;
        this.fingerTable.set(0, new PeerInfo(address, Peer.calculateID(address)));

        this.stabilizer = Executors.newSingleThreadScheduledExecutor();
        this.fingerFixer = Executors.newSingleThreadScheduledExecutor();
        this.predecessorChecker = Executors.newSingleThreadScheduledExecutor();

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

        if (id > this.getId() && id <= fingerTable.get(0).getId()) {
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
            if (fingerTable.get(i).getId() < id && fingerTable.get(i).getId() > this.getId()) {
                return fingerTable.get(i);
            }
        }
        return this.info;
    }

    public PeerInfo getPredecessor() {
        return this.predecessor;
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

        if (ni != null && (ni.getId() < this.fingerTable.get(0).getId() && ni.getId() > this.getId())) {
            fingerTable.set(0, ni);
        }

        if(ni != null && ( this.fingerTable.get(0).getId() == this.getId()))
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
        if (this.predecessor == null || (node.getId() > this.predecessor.getId() && node.getId() < this.info.getId())) {
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
            fingerTable.set(next - 1, findSuccessor((this.getId() + (int) Math.pow(2, next - 1)) % (int)Math.pow(2, m)));
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

    public void communicate() throws IOException {
        while (true) {
            Socket socket = this.serverSocket.accept();

            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            System.out.println("Accepted");

            byte[] readData = new byte[64000];
            int nRead = in.read(readData, 0, 64000);

            byte[] aux = Arrays.copyOfRange(readData, 0, nRead);

            System.out.println(nRead);

            String[] data = (new String(aux)).split(" ");

            System.out.println(data[1].split("\0")[0]);
        }

    }


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

        peer.communicate();
    }

}
