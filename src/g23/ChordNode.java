package g23;

import java.rmi.RemoteException;
import java.rmi.Remote;

public interface ChordNode extends Remote {

    PeerInfo findSuccessor(long id) throws RemoteException;

    boolean notify(PeerInfo node) throws RemoteException;

    boolean isAlive() throws RemoteException;

    PeerInfo getPredecessor() throws RemoteException;
}
