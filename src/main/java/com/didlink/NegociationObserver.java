package com.didlink;

import io.vos.stun.ice.Peer;

import java.net.Socket;

/**
 * Created by wuh56 on 4/25/2017.
 */
public interface NegociationObserver {
    public void onSuccess(String remoteAddress, int remotePort);
    public void onSuccess(Peer peer);
    public void onError(String remoteAddress, int remotePort);
    public void onTimeout(String remoteAddress, int remotePort);
}
