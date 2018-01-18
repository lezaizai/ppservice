package io.vos.stun.demo;

import java.net.DatagramSocket;
import java.net.Socket;

/**
 * Created by wuh56 on 4/24/2017.
 */
public interface TcpEstablishedListener {
    public void established(String publicAddress, int publicPort, int localPort);
    public void onError();
}
