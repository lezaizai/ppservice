package io.vos.stun.protocol;

import java.net.InetSocketAddress;

public interface MessageHandler {

  void onMessage(
          byte[] messageData, InetSocketAddress remoteAddress, ResponseHandler responseHandler);

}
