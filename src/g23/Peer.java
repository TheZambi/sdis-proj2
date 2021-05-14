package g23;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Peer {

    private static final int m = 256;

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
        for (int i = 0; i < m; i++)
            fingerTable.add(this.info);

        this.serverSocket = new ServerSocket(address.getPort());

        this.predecessor = null;
        this.successor = new PeerInfo(address, Peer.calculateID(address));
    }

    private static long calculateID(InetSocketAddress address) {

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] hash = digest.digest(address.toString().getBytes());
        UUID id = UUID.nameUUIDFromBytes(hash);
        long idInt = id.getLeastSignificantBits() * -1;
        idInt = Math.round(idInt % Math.pow(2, Peer.m));
        return idInt;
    }

    private PeerInfo findSuccessor(long id) {

        if (id > this.getId() && id <= fingerTable.get(0).getId()){
            return fingerTable.get(0);
        } else {
            PeerInfo closestPeer = this.closestPrecedingPeer(id);
//            System.out.println(closestPeer.getId());
//            System.out.println("ID:" + id);
//            System.out.println("This id: " + this.info.getId());
            if(closestPeer.getId() == this.info.getId()) {
                return this.info;
            }
            try(Socket socket = new Socket(closestPeer.getAddress().getAddress(), closestPeer.getAddress().getPort());
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                BufferedInputStream in = new BufferedInputStream(socket.getInputStream())
            ) {
                out.write(("GETSUCCESSOR " + getId()).getBytes());
                out.flush();
                ObjectInputStream infoInObject = new ObjectInputStream(in);
                PeerInfo successorInfo = (PeerInfo) infoInObject.readObject();
                return successorInfo;
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


    public void join(PeerInfo peer) {
        predecessor = null;
        try (Socket socket = new Socket(peer.getAddress().getAddress(), peer.getAddress().getPort());
             BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
             BufferedInputStream in = new BufferedInputStream(socket.getInputStream())
        ) {
            System.out.println(socket);
            String toSend = "GETSUCCESSOR " + getId();
            System.out.println(toSend);
            out.write(toSend.getBytes());
            out.flush();
            ObjectInputStream infoInObject = new ObjectInputStream(in);

            successor = (PeerInfo) infoInObject.readObject();

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("JOINED - SUCCESSOR: " + this.successor.getId());
        //find successor of node address //Maybe send message
    }


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

            byte[] aux = Arrays.copyOfRange(readData,0, nRead);

            System.out.println(nRead);

            String[] data = (new String(aux)).split(" ");

            System.out.println(data[1].split("\0")[0]);

            switch (data[0]) {
                case "NOTIFY" -> getNotified(new PeerInfo((InetSocketAddress) socket.getRemoteSocketAddress(), Peer.calculateID((InetSocketAddress) socket.getRemoteSocketAddress())));
                case "GETSUCCESSOR" -> {
                    BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                    ObjectOutputStream outObject = new ObjectOutputStream(out);
                    outObject.writeObject(this.findSuccessor(Long.parseLong(data[1]))); // Needs to send peerinfo
                    outObject.flush();
                    socket.close();
                }
            }
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
