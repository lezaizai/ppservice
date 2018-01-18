package io.vos.stun.demo;

import com.google.common.base.Preconditions;
import io.vos.stun.attribute.*;
import io.vos.stun.message.Message;
import io.vos.stun.protocol.Agent;
import io.vos.stun.protocol.ResponseHandler;
import io.vos.stun.util.Bytes;

import java.io.IOException;
import java.net.*;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import static io.vos.stun.message.Messages.MESSAGE_CLASS_REQUEST;
import static io.vos.stun.message.Messages.MESSAGE_METHOD_BINDING;

public class UdpStunClient {
//  Timer timer;
//  String stunServer = "www.disneyfans.cn";
//  int port = 3478;
  int timeout = 500; //ms

  public void tryTest(String stunServer, int stunPort, UdpEstablishedListener udpEstablishedListener) {
//    timer = new Timer(true);
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    DatagramClient task = new DatagramClient(stunServer, stunPort, timeout, udpEstablishedListener);

    executor.schedule(task, 10, TimeUnit.MILLISECONDS);
    executor.shutdown();
    //timer.schedule(task, 10);

  }

  private class FollowTask extends TimerTask {
    private UdpEstablishedListener udpEstablishedListener;
    String publicAddress;
    int publicPort;
    int localPort;
    DatagramSocket dgramSocket;

    public FollowTask(String publicAddress,
                      int publicPort,
                      int localPort,
                      DatagramSocket dgramSocket,
                      UdpEstablishedListener udpEstablishedListener) {
      super();
      this.publicAddress = publicAddress;
      this.publicPort = publicPort;
      this.localPort = localPort;
      this.udpEstablishedListener = udpEstablishedListener;
      this.dgramSocket = dgramSocket;
    }

    public void run() {
      if (this.udpEstablishedListener != null)
        udpEstablishedListener.established(dgramSocket, publicAddress, publicPort, localPort);
    }
  }

  private class ErrorNotify extends TimerTask {
    private UdpEstablishedListener udpEstablishedListener;

    public ErrorNotify(UdpEstablishedListener udpEstablishedListener) {
      super();
      this.udpEstablishedListener = udpEstablishedListener;
    }

    public void run() {
      if (this.udpEstablishedListener != null)
        udpEstablishedListener.onError();
    }
  }

  private class DatagramClient extends TimerTask {

    private String stunServer;
    private int serverPort;
    private int timeout;
    private final Agent agent;
    private UdpEstablishedListener udpEstablishedListener;

    DatagramClient(String stunServer, int serverPort, int timeout, UdpEstablishedListener udpEstablishedListener) {
      super();
      this.stunServer = stunServer;
      this.serverPort = serverPort;
      this.timeout = timeout;
      this.agent = Agent.createBasicServer();
      this.udpEstablishedListener = udpEstablishedListener;
    }

    public void run() {

      DatagramSocket dgramSocket = null;
      boolean isError = false;
      try {
        dgramSocket = new DatagramSocket();
        dgramSocket.setReuseAddress(true);
//        dgramSocket.connect(InetAddress.getByName(stunServer), serverPort);
        dgramSocket.setSoTimeout(timeout);

        System.out.println(String.format("Started datagram client on %s %d ", dgramSocket.getLocalAddress(), dgramSocket.getLocalPort()));

        Message request = Message.builder()
                .setMessageClass(MESSAGE_CLASS_REQUEST)
                .setMessageMethod(MESSAGE_METHOD_BINDING)
                .generateTransactionID()
                .build();
        byte[] requestBytes = request.getBytes();
        DatagramPacket replyPacket = new DatagramPacket(
                requestBytes, requestBytes.length);
        replyPacket.setSocketAddress(new InetSocketAddress(stunServer, serverPort));
        dgramSocket.send(replyPacket);

        byte[] packetBuffer = new byte[1024];

        while (true) {
          DatagramPacket dgramPacket = new DatagramPacket(packetBuffer, packetBuffer.length);

          dgramSocket.receive(dgramPacket);
          Message response = new Message(Preconditions.checkNotNull(packetBuffer));

          if (request.equalTransactionID(response)) {
            int packetLen = dgramPacket.getLength();
//            System.out.println(String.format("Received packet of size %d bytes", packetLen));
            byte[] msgBuffer = new byte[packetLen];
            System.arraycopy(packetBuffer, 0, msgBuffer, 0, packetLen);

            final InetSocketAddress remoteAddress =
                    new InetSocketAddress(dgramPacket.getAddress(), dgramPacket.getPort());
System.out.println(String.format("Received message from %s %d", dgramPacket.getAddress(), dgramPacket.getPort()));
            ResponseHandler rh =
                    createResponseHandler(dgramSocket);

            agent.onMessage(msgBuffer, remoteAddress, rh);
            break;
          }
        }

      } catch (UnknownHostException s) {
        System.out.println("Unknown stun server " + stunServer);
        s.printStackTrace();
        isError = true;
      } catch (PortUnreachableException s) {
        System.out.println("Unreachable stun server " + stunServer);
        s.printStackTrace();
        isError = true;
      } catch (SocketTimeoutException s) {
        System.out.println("Timeout stun server " + stunServer);
        s.printStackTrace();
        isError = true;
      } catch (SocketException s) {
        System.out.println("Unable to create new datagram socket");
        s.printStackTrace();
        isError = true;
      } catch (IOException e) {
        System.out.println("Unable to send to / receive from stun server " + stunServer);
        e.printStackTrace();
        isError = true;
      } finally {
      }

      if (isError && udpEstablishedListener != null) {
        if (dgramSocket != null) {
          if (dgramSocket.isConnected()) dgramSocket.disconnect();
          if (!dgramSocket.isClosed()) dgramSocket.close();
        }
        if (this.timeout > 3000) {
          ErrorNotify task = new ErrorNotify(udpEstablishedListener);
          //timer.schedule(task, 10);
          ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
          executor.schedule(task, 10, TimeUnit.MILLISECONDS);
          executor.shutdown();

          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          this.timeout = 500;
        }
        System.out.println("Failed. Re-try.... ");

        DatagramClient task = new DatagramClient(this.stunServer, this.serverPort, this.timeout*2, this.udpEstablishedListener);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.schedule(task, 2000, TimeUnit.MILLISECONDS);
        executor.shutdown();

          //new Thread(task).start();
          //timer.schedule(task, 10);
//        } else {
//          ErrorNotify task = new ErrorNotify(udpEstablishedListener);
//          timer.schedule(task, 10);
//        }
      }

    }

    private int getPaddedLength(int length) {
      int remainder = length % 4;
      return remainder == 0 ? length : length + 4 - remainder;
    }

    private ResponseHandler createResponseHandler(
            final DatagramSocket dgramSocket) {
      return new ResponseHandler() {
        @Override
        public void onQuest(byte[] messageData, InetAddress destAddress, int destPort) {

        }

        @Override
        public void onResponse(byte[] messageData, InetAddress destAddress, int destPort) {

          int currentByte = 0;
          int type = Bytes.twoBytesToInt(messageData[currentByte++], messageData[currentByte++]);
          int length = Bytes.twoBytesToInt(messageData[currentByte++], messageData[currentByte++]);

          byte[] valueData;
          if (length > 0) {
            int paddedLength = getPaddedLength(length);
            valueData = new byte[paddedLength];
            // we can just copy to length, because the valueData array is already
            // initialized to 0 byte values
            System.arraycopy(messageData, currentByte, valueData, 0, length);
          } else {
            valueData = new byte[0];
          }

          AttributeFactory factory = new RFC5389AttributeFactory();
          MappedAddressAttribute mappedAttribute = (MappedAddressAttribute)
                  factory.createAttribute(type, length, valueData);

          try {
            InetAddress mappedAddr = InetAddress.getByAddress(mappedAttribute.getMappedAddress());

            FollowTask task = new FollowTask(mappedAddr.getHostAddress(),
                    mappedAttribute.getPort(),
                    dgramSocket.getLocalPort(),
                    dgramSocket,
                    udpEstablishedListener);
            //timer.schedule(task, 10);
            ExecutorService executor = Executors.newFixedThreadPool(1);
            executor.submit(task);
            executor.shutdown();

            System.out.println(String.format("Received Mapped Address: %s %d", mappedAddr.getHostAddress(), mappedAttribute.getPort()));
          } catch (UnknownHostException e) {
            e.printStackTrace();
          }

        }

        @Override
        public void onIndication(byte[] messageData, InetAddress destAddress, int destPort) {

        }
      };
    }
  }

}
