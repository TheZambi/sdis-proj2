package g23;

import java.rmi.RemoteException;

public interface ChordNode {

    PeerInfo findSuccessor(long id) throws RemoteException;
}
