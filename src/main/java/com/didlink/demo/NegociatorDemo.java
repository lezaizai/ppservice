package com.didlink.demo;

/**
 * Created by wuh56 on 4/25/2017.
 */
public class NegociatorDemo {
    public static void main(String[] argv) throws Exception {
        UdpNegociator udpNegociator = new UdpNegociator();
        udpNegociator.start();
//        TcpNegociator tcpNegociator = new TcpNegociator();
//        tcpNegociator.start();

    }
}
