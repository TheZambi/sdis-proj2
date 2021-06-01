package g23.Messages;

import java.io.Serializable;

public class Message implements Serializable {

    private MessageType type;
    private long senderId;
    private long fileId;
    private int replicationDegree;
    private int currentReplicationsDegree;
    private byte[] body;
    private String address;
    private int port;
    private long fileSize;
    private boolean isSeen = false;

    public Message(MessageType type, String[] args, byte[] body) {
        this.type = type;
        this.senderId = Long.parseLong(args[0]);
        this.fileId = Long.parseLong(args[1]);
        this.body = body;

        if (type == MessageType.PUTFILE) {
            this.replicationDegree = Integer.parseInt(args[2]);
            this.currentReplicationsDegree = Integer.parseInt(args[3]);
            this.fileSize = Long.parseLong(args[4]);
            this.address = args[5];
            this.port = Integer.parseInt(args[6]);
        } else if (type == MessageType.GETFILE) {
            this.address = args[2];
            this.port = Integer.parseInt(args[3]);
        } else if (type == MessageType.REMOVED) {
            this.replicationDegree = Integer.parseInt(args[2]);
            this.currentReplicationsDegree = Integer.parseInt(args[3]);
        } else if (type != MessageType.DELETE && type != MessageType.DELETED) {
            this.replicationDegree = -1;
            this.currentReplicationsDegree = -1;
        } else if (type == MessageType.DELETE) {
            this.replicationDegree = -1;
            this.currentReplicationsDegree = Integer.parseInt(args[2]);
        } else if (type == MessageType.IDELETED) {
            this.replicationDegree = -1;
            this.currentReplicationsDegree = -1;
        }
    }

    public boolean isSeen() {
        return this.isSeen;
    }

    public void seeMessage() {
        this.isSeen = true;
    }

    public int getCurrentReplicationDegree() {
        return currentReplicationsDegree;
    }

    public void decrementCurrentReplication() {
        this.currentReplicationsDegree--;
    }

    public byte[] getBody() {
        return body;
    }

    public MessageType getType() {
        return type;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getFileId() {
        return fileId;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public String getAddress() {
        return address;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getPort() {
        return port;
    }
}
