package io.vos.stun.ice;

import java.net.InetAddress;
import java.net.Socket;
import java.sql.Timestamp;

import static io.vos.stun.ice.Peers.NODE_STATUS_INIT;

/**
 * Created by wuh56 on 4/25/2017.
 */
public class Peer {
    private Socket socket;
    private String id;
    private String address;
    private int port;
    private boolean isCaller;
    private boolean requestReceived;
    private boolean responseReceived;
    private int status;
    private Timestamp lastUpdate;

    public Peer(String address, int port, boolean isCaller) {
        this.id = String.format("%s:%d", address, port);
        this.address = address;
        this.port = port;
        this.isCaller = isCaller;
        this.requestReceived = false;
        this.responseReceived = false;
        this.status = NODE_STATUS_INIT;
        this.lastUpdate = new Timestamp(System.currentTimeMillis());
    }

    public Peer(InetAddress address, int port, boolean isCaller) {
        this.id = String.format("%s:%d", address, port);
        this.address = address.getHostAddress();
        this.port = port;
        this.isCaller = isCaller;
        this.requestReceived = false;
        this.responseReceived = false;
        this.status = NODE_STATUS_INIT;
        this.lastUpdate = new Timestamp(System.currentTimeMillis());
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public String getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public String getId(){
        return this.id;
    }

    public void setRequestReceived() {
        this.requestReceived = true;
    }

    public void setResponseReceived() {
        this.responseReceived = true;
    }

    public boolean getRequestReceived() {
        return this.requestReceived;
    }

    public boolean getResponseReceived() {
        return this.responseReceived;
    }

    public boolean isSucceed() {
        return this.requestReceived || this.responseReceived;
    }

    public boolean isTimeout() {
        return (new Timestamp(System.currentTimeMillis())).getTime() - this.lastUpdate.getTime() > 1000*60*5;
    }
}
