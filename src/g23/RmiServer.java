package g23;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class RmiServer {
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
//        System.setProperty("java.rmi.server.hostname", "25.2.54.55");
        Registry reg = LocateRegistry.createRegistry(1099);

        ServerSocket serverSocket = new ServerSocket(8099);


        int[] a = {5, 10};
        int i = 0;
        while(true) {
            Socket sock = serverSocket.accept();
            System.out.println(a[i]);

            try (ObjectInputStream ois = new ObjectInputStream(sock.getInputStream())) {
                ChordNode cn = (ChordNode) ois.readObject();
                Registry registry = LocateRegistry.getRegistry();
                registry.rebind(String.valueOf(a[i]), cn);
                sock.close();
            }
            System.out.println(Arrays.toString(reg.list()));
            i++;
        }
    }
}
