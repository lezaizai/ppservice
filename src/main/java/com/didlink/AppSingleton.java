package com.didlink;

import io.vos.stun.ice.PeerCollection;

import java.net.DatagramSocket;
import java.net.Socket;

/**
 * Created by wuh56 on 4/25/2017.
 */
public class AppSingleton {
    private static AppSingleton instance;
    public static AppSingleton getInstance() {
        if (instance == null) {
            instance = new AppSingleton();
        }
        return instance;
    }

//    public static String stunServer = "www.disneyfans.cn";
//    public static String stunServer = "localhost";
    public static String stunServer = "10.86.130.136";

    public static int stunPort = 3479;

    private PeerCollection nodeCollection = PeerCollection.EMPTY_COLLECTION;

    public PeerCollection getNodeCollection() {
        return this.nodeCollection;
    }
    public void setNodeCollection(PeerCollection nodeCollection) {
        this.nodeCollection = nodeCollection;
    }

    private SendService sendService;

    public void setSendService(SendService sendService) {
        this.sendService = sendService;
    }

    public SendService getSendService() {
        return sendService;
    }

    private DatagramSocket dgramSocket;
    public void setDgramSocket(DatagramSocket dgramSocket) {
        this.dgramSocket = dgramSocket;
    }

    public DatagramSocket getDgramSocket() {
        return this.dgramSocket;
    }

    private Socket socket;
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return this.socket;
    }

}
