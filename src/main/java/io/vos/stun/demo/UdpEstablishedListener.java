package io.vos.stun.demo;

import java.net.DatagramSocket;

/**
 * Created by wuh56 on 4/24/2017.
 */
public interface UdpEstablishedListener {
    public void established(DatagramSocket dgramSocket, String publicAddress, int publicPort, int localPort);
    public void onError();
}
