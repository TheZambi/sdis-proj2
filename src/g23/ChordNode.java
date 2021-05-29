package g23;

import java.rmi.RemoteException;
import java.rmi.Remote;

public interface ChordNode extends Remote {

    PeerInfo findSuccessor(long id) throws RemoteException;

    boolean notify(PeerInfo node) throws RemoteException;

    boolean isAlive() throws RemoteException;

    PeerInfo getPredecessor() throws RemoteException;

    PeerInfo getSuccessor() throws RemoteException;

    void backup(String path, int replicationDegree) throws RemoteException;

    void restore(String path) throws RemoteException;

    void delete(String path) throws RemoteException;

    void reclaim(long amountOfBytes) throws RemoteException;

    PeerState state() throws RemoteException;
}
