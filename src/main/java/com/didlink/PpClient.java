package com.didlink;

import io.vos.stun.demo.UdpEstablishedListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class PpClient {


    public static void main(String[] argv) throws IOException {
//        timer = new Timer(true);

        UdpEstablishedListener udpEstablishedListener = new UdpEstablishedListener() {
            @Override
            public void established(String publicAddress, int publicPort, int localPort) {

                System.out.println(String.format("UdpEstablishedListener public address %s %d, local port %d", publicAddress, publicPort, localPort));

            }

            @Override
            public void onError() {
            }

        };

        String localIP = InetAddress.getLocalHost().getHostAddress();
        System.out.println(String.format("Local IP address: %s",localIP));

        UdpPpClient udpPpClient = new UdpPpClient();
        udpPpClient.tryTest("127.0.0.1", 7366, udpEstablishedListener);

    }

}
