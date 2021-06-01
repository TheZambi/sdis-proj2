package g23.SSLEngine;

import javax.net.ssl.*;
import javax.print.DocFlavor;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SSLEngineOrchestrator {

    private static final String serverKeysFile = "../../keys/server.keys"; //TODO ?
    private static final String clientKeysFile = "../../keys/client.keys"; //TODO ?
    private static final String truststoreFile = "../../keys/truststore"; //TODO ?

    protected final SSLContext sslContext;
    protected final SSLEngine sslEngine;
    protected final SocketChannel socketChannel;

    protected ByteBuffer netInBuf;
    protected ByteBuffer netOutBuf;

    protected ByteBuffer appInBuf;
    protected ByteBuffer appOutBuf;

    protected final ExecutorService taskExecutor;

    public SSLEngineOrchestrator(SocketChannel socketChannel, SSLContext sslContext, InetSocketAddress address, boolean isClient) {
        this.socketChannel = socketChannel;
        this.sslContext = sslContext;
        this.sslEngine = sslContext.createSSLEngine(address.getHostName(), address.getPort());
        this.sslEngine.setUseClientMode(isClient);
        this.sslEngine.setNeedClientAuth(true);

        try {
            this.socketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SSLSession sslSession = this.sslEngine.getSession();
        int netBufSize = sslSession.getPacketBufferSize();
        int appBufSize = sslSession.getApplicationBufferSize();

        netInBuf = ByteBuffer.allocate(netBufSize);
        netOutBuf = ByteBuffer.allocate(netBufSize);
        appInBuf = ByteBuffer.allocate(appBufSize);
        appOutBuf = ByteBuffer.allocate(appBufSize);

        taskExecutor = Executors.newSingleThreadExecutor();
    }

    public void doHandshake() {
        try {
            sslEngine.beginHandshake();
            SSLEngineResult.HandshakeStatus status = sslEngine.getHandshakeStatus();

            while(status != SSLEngineResult.HandshakeStatus.FINISHED && status != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                switch (sslEngine.getHandshakeStatus()) {
                    case NEED_UNWRAP:
                        status = this.need_unwrap();
                        break;
                    case NEED_WRAP:
                        status = this.need_wrap();
                        break;
                    case NEED_TASK:
                        Runnable task;
                        while ((task = sslEngine.getDelegatedTask()) != null) {
                            taskExecutor.execute(task);
                        }
                        status = sslEngine.getHandshakeStatus();
                        break;
                    case FINISHED:
                        break;
                    case NOT_HANDSHAKING:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("FINISHED HANDSHAKING YEY");
    }

    public SSLEngineResult.HandshakeStatus need_unwrap() {
        netInBuf.flip();
        SSLEngineResult result = null;
        try {
            result = sslEngine.unwrap(netInBuf, appInBuf);
        } catch (SSLException e) {
            e.printStackTrace();
        }
        netInBuf.compact();
        SSLEngineResult.HandshakeStatus status = result.getHandshakeStatus();

        switch(result.getStatus()){
            case BUFFER_OVERFLOW:
                if(sslEngine.getSession().getApplicationBufferSize() > appInBuf.capacity()){
                    appInBuf = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
                } else{
                    appInBuf.clear();
                }
                return this.need_unwrap();
            case BUFFER_UNDERFLOW:
                if(sslEngine.getSession().getPacketBufferSize() > netInBuf.capacity()){
                    netInBuf = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
                } else{
                    netInBuf.clear();
                }

                try {
                    socketChannel.read(netInBuf);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return this.need_unwrap();
            case CLOSED:
                try {
                    socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return status;
            case OK:
                return status;
        }
        return status;
    }

    public byte[] read() throws IOException {
        int num = socketChannel.read(netInBuf);
        if(num == 0){
            this.read();
        } else{
            this.need_unwrap();
        }

        this.appInBuf.flip();

        int bytesRead = this.appInBuf.remaining();

        byte[] messageRead = new byte[bytesRead];
        this.appInBuf.get(messageRead,0,bytesRead);
        return messageRead;
    }

    public void write(byte[] message){
        this.appOutBuf.put(message);

        try {
            this.need_wrap();
        } catch (SSLException e) {
            e.printStackTrace();
        }
    }

    public void shutdown(){
        sslEngine.closeOutbound();

//        while(!sslEngine.isOutboundDone()){
//            SSLEngineResult res = sslEngine.wrap(empty,netOutBuf);
//
//            while(netOutBuf.hasRemaining()){
//                int num = 0;
//                try {
//                    num = socketChannel.write(netOutBuf);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                if(num == 0){
//                    continue;
//                }
//                netOutBuf.compact();
//            }
//        }
        doHandshake();

        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SSLEngineResult.HandshakeStatus need_wrap() throws SSLException {
        netOutBuf.clear();

        SSLEngineResult result = null;
        try {
            result = sslEngine.wrap(netOutBuf, appOutBuf);
        } catch (SSLException e) {
            e.printStackTrace();
        }
        SSLEngineResult.HandshakeStatus status = result.getHandshakeStatus();

        switch(result.getStatus()){
            case BUFFER_OVERFLOW:
                if(sslEngine.getSession().getApplicationBufferSize() > appOutBuf.capacity()){
                    appOutBuf = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
                } else{
                    appOutBuf.clear();
                }
                return this.need_wrap();
            case BUFFER_UNDERFLOW:
                throw new SSLException("Buffer underflow after wrap");
            case CLOSED:
                try {
                    socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return status;
            case OK:
                return status;
        }
        return status;
    }

    public static SSLContext createContext(boolean client, String pass) throws Exception {
        KeyStore keystore = KeyStore.getInstance("JKS");
        KeyStore truststore = KeyStore.getInstance("JKS");

        char[] passphrase = pass.toCharArray();

        keystore.load(new FileInputStream(client ? clientKeysFile : serverKeysFile), passphrase);
        truststore.load(new FileInputStream(truststoreFile), passphrase);

        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyFactory.init(keystore, passphrase);

        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(truststore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyFactory.getKeyManagers(), trustFactory.getTrustManagers(), null);
        return sslContext;
    }
}
