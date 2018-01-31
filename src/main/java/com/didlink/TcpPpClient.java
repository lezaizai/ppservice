package com.didlink;

import com.google.common.base.Preconditions;
import io.vos.stun.attribute.AttributeFactory;
import io.vos.stun.attribute.MappedAddressAttribute;
import io.vos.stun.attribute.RFC5389AttributeFactory;
import io.vos.stun.demo.EstablishListener;
import io.vos.stun.message.Message;
import io.vos.stun.protocol.Agent;
import io.vos.stun.protocol.ResponseHandler;
import io.vos.stun.util.Bytes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.vos.stun.message.Messages.MESSAGE_CLASS_REQUEST;
import static io.vos.stun.message.Messages.MESSAGE_METHOD_BINDING;

public class TcpPpClient {
//  Timer timer;
//  String stunServer = "www.disneyfans.cn";
//  int port = 3479;
  int timeout = 5000; //ms

  public void tryTest(String stunServer, int stunPort, EstablishListener establishListener) {
//    timer = new Timer(true);
    ThreadedClient task = new ThreadedClient(stunServer, stunPort, timeout, establishListener);
    ExecutorService executor = Executors.newFixedThreadPool(1);
    executor.submit(task);
    executor.shutdown();
    //timer.schedule(task, 10);

  }

  private class FollowTask extends TimerTask {
    private EstablishListener establishListener;
    String publicAddress;
    int publicPort;
    int localPort;

    public FollowTask(String publicAddress,
                      int publicPort,
                      int localPort,
                      EstablishListener establishListener) {
      super();
      this.publicAddress = publicAddress;
      this.publicPort = publicPort;
      this.localPort = localPort;
      this.establishListener = establishListener;
    }

    public void run() {
      if (this.establishListener != null)
        establishListener.established(publicAddress, publicPort, localPort);
    }
  }

  private class ErrorNotify extends TimerTask {
    private EstablishListener establishListener;

    public ErrorNotify(EstablishListener establishListener) {
      super();
      this.establishListener = establishListener;
    }

    public void run() {
      if (this.establishListener != null)
        establishListener.onError();
    }
  }

  private class ThreadedClient extends TimerTask {

    private String stunServer;
    private int serverPort;
    private int timeout;
    private final Agent agent;
    private EstablishListener establishListener;

    ThreadedClient(String stunServer, int serverPort, int timeout, EstablishListener establishListener) {
      super();
      this.stunServer = stunServer;
      this.serverPort = serverPort;
      this.timeout = timeout;
      this.agent = Agent.createBasicServer();
      this.establishListener = establishListener;
    }

    public void run() {

      Socket socket = null;
      boolean isError = false;
      try {
        socket = new Socket();
        socket.setReuseAddress(true);
        socket.connect(new InetSocketAddress(stunServer, serverPort));
        socket.setSoTimeout(timeout);

        System.out.println(String.format("Started tcp socket client on %s %d ", socket.getLocalAddress(), socket.getLocalPort()));
        DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
        final DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

        Message request = Message.builder()
                .setMessageClass(MESSAGE_CLASS_REQUEST)
                .setMessageMethod(MESSAGE_METHOD_BINDING)
                .generateTransactionID()
                .build();
        byte[] requestBytes = request.getBytes();
        outToServer.write(requestBytes);
        outToServer.flush();

        byte[] buffer = new byte[4];
        while (!socket.isClosed()) {
          inFromServer.read(buffer);

          int totalBytes = agent.totalBytesInMessage(buffer);
          byte[] msgBuffer = new byte[totalBytes];
          System.arraycopy(buffer, 0, msgBuffer, 0, buffer.length);
          if (totalBytes > buffer.length) {
            inFromServer.read(msgBuffer, 4, totalBytes - 4);
          }
          Message response = new Message(Preconditions.checkNotNull(msgBuffer));

          if (request.equalTransactionID(response)) {

            InetSocketAddress remoteAddress =
                    new InetSocketAddress(socket.getInetAddress(), socket.getPort());
            System.out.println(String.format("Received message from %s %d", remoteAddress.getAddress(), remoteAddress.getPort()));
            ResponseHandler rh =
                    createResponseHandler(socket);

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
        if (socket != null) {
          try {
             if (!socket.isClosed()) socket.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

      if (isError && establishListener != null) {
        if (this.timeout > 3000) {
          ErrorNotify task = new ErrorNotify(establishListener);
          //timer.schedule(task, 10);
          ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
          executor.schedule(task, 10, TimeUnit.MILLISECONDS);
          executor.shutdown();

        } else {
          System.out.println("Failed. Re-try.... ");

          ThreadedClient task = new ThreadedClient(this.stunServer, this.serverPort, this.timeout * 2, this.establishListener);

          ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
          executor.schedule(task, 2000, TimeUnit.MILLISECONDS);
          executor.shutdown();
        }
      }

    }

    private int getPaddedLength(int length) {
      int remainder = length % 4;
      return remainder == 0 ? length : length + 4 - remainder;
    }

    private ResponseHandler createResponseHandler(
            final Socket socket) {
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
                    socket.getLocalPort(),
                    establishListener);
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
