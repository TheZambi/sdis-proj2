package g23;

import g23.Messages.Message;
import g23.Protocols.ReceiveChunk;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class MessageInterpreter implements Runnable {
    Peer peer;
    Socket socket;
    public MessageInterpreter(Peer peer, Socket socket) {
        this.peer = peer;
        this.socket = socket;
    }

    @Override
    public void run() {
        byte[] readData = new byte[64000];

        try (BufferedInputStream in = new BufferedInputStream(this.socket.getInputStream())) {
            int nRead = in.read(readData, 0, 64000);
            byte[] aux = Arrays.copyOfRange(readData, 0, nRead);
            Message message = new Message(aux);
//            System.out.println(data[1].split("\0")[0]);
            switch (message.getType()) {
                case PUTCHUNK:
                    (new ReceiveChunk(this.peer, message, socket)).handleMessage();

                    break;
            }


            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
