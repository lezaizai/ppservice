package com.didlink;

import io.vos.stun.demo.EstablishListener;

import java.io.IOException;
import java.net.InetAddress;

import static java.lang.Thread.sleep;

public class PpClient {


    public static void main(String[] argv) throws Exception {
//        timer = new Timer(true);

        EstablishListener udpEstablishedListener = new EstablishListener() {
            @Override
            public void established(String publicAddress, int publicPort, int localPort) {

                System.out.println(String.format("EstablishedListener public address %s %d, local port %d", publicAddress, publicPort, localPort));

            }

            @Override
            public void onError() {
            }

        };

        String localIP = InetAddress.getLocalHost().getHostAddress();
        System.out.println(String.format("Local IP address: %s",localIP));

        for (int i=0; i<5; i++) {
            UdpPpClient udpPpClient = new UdpPpClient();
            udpPpClient.tryTest("127.0.0.1", 7366, 122, 3434.4343434D, 434.4344455D, 43434555665544L, udpEstablishedListener);
            sleep(5000);
        }
    }

}
