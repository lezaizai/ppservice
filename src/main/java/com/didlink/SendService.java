package com.didlink;

/**
 * Created by wuh56 on 4/24/2017.
 */
public interface SendService {
    public void sendMessage(String msg, String remoteAddr, int remotePort);
    public void sendMessage(byte[] msgBytes, String remoteAddr, int remotePort);
    public void sendTcpMessage(String msg);
    public void sendTcpMessage(byte[] msgBytes);
}
