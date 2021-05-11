package g23;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Peer {

    private static final int m = 32;

    private PeerInfo info;

    private List<PeerInfo> fingerTable;
    private HashMap<String, String> data; //TODO

    private PeerInfo predecessor;
    private PeerInfo successor;

    private int next; //Used for fix_fingers method

    private ServerSocket serverSocket;

    public Peer(InetSocketAddress address) throws IOException {

        this.info = new PeerInfo(address, Peer.calculateID(address));
        this.fingerTable = new ArrayList<>();
        this.serverSocket = new ServerSocket(address.getPort());

        this.predecessor = null;
        this.successor = new PeerInfo(address, Peer.calculateID(address));
    }

    private static int calculateID(InetSocketAddress address) {

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] hash = digest.digest(address.toString().getBytes());
        return 0; //TODO
    }

    private PeerInfo findSuccessor(Integer id) {
        if (id > this.getId() && id < fingerTable.get(0).getId())
            return fingerTable.get(0);
        else {
            for (int i = fingerTable.size(); i >= 0; i--) {
                if (fingerTable.get(0).getId() < id && fingerTable.get(0).getId() > this.getId()) {
                    return fingerTable.get(0);
                }
            }
        }
        return this.info;
    }


    private void join(PeerInfo address) {
        //find successor of node address //Maybe send message
    }

//    private void stabilize() {
////        Map.Entry<Integer, InetSocketAddress> x = successor.getPredecessor();// -->> Needs to send message
//        if(this.getId() < x.getKey() && x.getKey() < successor.getId()){
//            successor = x;
//        }
//
////        this.notify(successor);
//    }

    //getNotified
    private void getNotified(PeerInfo node) {
        if (predecessor == null || (predecessor.getId() < node.getId() && node.getId() < this.getId())) {
            predecessor = node;
        }
    }

    private void fix_fingers() {
        next += 1;
        if (next > m) {
            next = 1;
        }
        fingerTable.set(next - 1, findSuccessor(this.getId() + (int) Math.pow(2, next - 1)));
    }


    public Integer getId() {
        return info.getId();
    }

    public InetSocketAddress getAddress() {
        return info.getAddress();
    }

    public void communicate() throws IOException {
        while (true) {
            Socket socket = this.serverSocket.accept();
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

            byte[] readData = new byte[64000];
            int nRead = in.read(readData, 0, 64000);

            String[] data = (new String(readData)).split(" ");

            switch (data[0]) {
                case "NOTIFY":
                    getNotified(new PeerInfo((InetSocketAddress) socket.getRemoteSocketAddress(), Peer.calculateID((InetSocketAddress) socket.getRemoteSocketAddress())));
                    break;
                case "GETSUCCESSOR":
                    Socket outSocket = new Socket(socket.getRemoteSocketAddress(), data[1]);
                    BufferedOutputStream out=new BufferedOutputStream(outSocket.getOutputStream());
                    out.write("SUCCESSOR"); // Needs to send peerinfo
                    break;
            }
        }
    }


    public static void main(String[] args) throws IOException, AlreadyBoundException {
//        if (args.length != 9) {
//            System.out.println("Usage: java Peer <protocol_version> <peer_id> <service_access_point> <MC_address> <MC_Port> <MDB_address> <MDB_Port> <MDR_address> <MDR_Port>");
//            return;
//        }
//
//        String protocolVersion = args[0];
//        if(!protocolVersion.equals("1.0") && !protocolVersion.equals("1.1") && !protocolVersion.equals("1.2") && !protocolVersion.equals("1.3") && !protocolVersion.equals("1.4")) {
//            System.out.println("Protocol versions available: 1.0, 1.1, 1.2, 1.3, 1.4");
//            return;
//        }

        int peerId = Integer.parseInt(args[1]);
        String serviceAccessPointName = args[2];
//
//        Channel MCchannel = new Channel(args[3], Integer.parseInt(args[4]));
//
//        Channel MDBchannel = new Channel(args[5], Integer.parseInt(args[6]));
//
//        Channel MDRchannel = new Channel(args[7], Integer.parseInt(args[8]));
//
//        g03.Peer peer = new g03.Peer(peerId, protocolVersion, serviceAccessPointName, MCchannel, MDBchannel, MDRchannel);
//
//        peer.synchronizer.scheduleAtFixedRate(new Synchronizer(peer), 0, 1, TimeUnit.SECONDS);

    }

}
