package com.didlink;

/**
 * Created by wuh56 on 4/25/2017.
 */
public class MainClass {
    public static void main(String[] argv) throws Exception {
        UdpNegociator udpNegociator = new UdpNegociator();
        udpNegociator.start();
//        TcpNegociator tcpNegociator = new TcpNegociator();
//        tcpNegociator.start();

    }
}
