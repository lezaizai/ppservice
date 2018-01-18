package io.vos.stun.testing;

import io.vos.stun.protocol.ResponseHandler;

import java.net.InetAddress;

public final class FakeResponseHandler implements ResponseHandler {

  byte[] messageData;

  @Override
  public void onQuest(byte[] messageData, InetAddress destAddress, int destPort) {

  }

  public void onResponse(byte[] messageData, InetAddress destAddress, int destPort) {
    this.messageData = messageData;
  }

  @Override
  public void onIndication(byte[] messageData, InetAddress destAddress, int destPort) {

  }
}
