package io.vos.stun.demo;

import io.vos.stun.protocol.Agent;
import io.vos.stun.protocol.ResponseHandler;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BasicServer {

  public static void main(String[] args) throws IOException {
//    MultiThreadedServer streamServer1 = new MultiThreadedServer(3478);
//    MultiThreadedServer streamServer2 = new MultiThreadedServer(3479);
    DatagramServer dgramServer1 = new DatagramServer(3478);
    DatagramServer dgramServer2 = new DatagramServer(3479);

//    new Thread(streamServer1).start();
//    new Thread(streamServer2).start();
//    new Thread(dgramServer1).start();
//    new Thread(dgramServer2).start();

    ExecutorService executor = Executors.newFixedThreadPool(2);
//    executor.submit(streamServer1);
//    executor.submit(streamServer2);
    executor.submit(dgramServer1);
    executor.submit(dgramServer2);
    executor.shutdown();
  }

  private static class DatagramServer implements Runnable {

    private int serverPort;
    private final Agent agent;

    DatagramServer(int serverPort) {
      this.serverPort = serverPort;
      this.agent = Agent.createBasicServer();
    }

    public void run() {

      final DatagramSocket dgramSocket;
      try {
        dgramSocket = new DatagramSocket(serverPort);
        System.out.println("Started datagram server on port " + serverPort);
      } catch (SocketException s) {
        System.out.println("Unable to create new datagram socket");
        s.printStackTrace();
        return;
      }

      byte[] packetBuffer = new byte[1024];
      while (true) {
        DatagramPacket dgramPacket = new DatagramPacket(packetBuffer, packetBuffer.length);

        try {
          dgramSocket.receive(dgramPacket);

          int packetLen = dgramPacket.getLength();
//          System.out.println(String.format("Received packet of size %d bytes", packetLen));
          byte[] msgBuffer = new byte[packetLen];
          System.arraycopy(packetBuffer, 0, msgBuffer, 0, packetLen);

          final InetSocketAddress remoteAddress =
              new InetSocketAddress(dgramPacket.getAddress(), dgramPacket.getPort());
          ResponseHandler rh =
              createResponseHandler(dgramSocket);
          System.out.println(String.format("Received packet from %s %d", dgramPacket.getAddress(), dgramPacket.getPort()));

          agent.onMessage(msgBuffer, remoteAddress, rh);
        } catch (IOException e) {
          System.out.println("Error receiving datagram packet");
          e.printStackTrace();
        }
      }
    }

    private ResponseHandler createResponseHandler(
        final DatagramSocket dgramSocket) {
      return new ResponseHandler() {
        @Override
        public void onQuest(byte[] messageData, InetAddress destAddress, int destPort) {
          System.out.println(String.format("Send packet to %s %d", destAddress.getHostAddress(), destPort));

          try {
            DatagramPacket replyPacket = new DatagramPacket(
                    messageData, messageData.length, destAddress, destPort);
            dgramSocket.send(replyPacket);
          } catch (IOException e) {
            System.out.println("Error writing response to client");
            e.printStackTrace();
          }
        }

        @Override
        public void onResponse(byte[] messageData, InetAddress destAddress, int destPort) {
          System.out.println(String.format("Send packet to %s %d", destAddress.getHostAddress(), destPort));

          try {
            DatagramPacket replyPacket = new DatagramPacket(
                messageData, messageData.length, destAddress, destPort);
            dgramSocket.send(replyPacket);
          } catch (IOException e) {
            System.out.println("Error writing response to client");
            e.printStackTrace();
          }
        }

        @Override
        public void onIndication(byte[] messageData, InetAddress destAddress, int destPort) {
          onResponse(messageData, destAddress, destPort);
        }
      };
    }
  }

  private static class MultiThreadedServer implements Runnable {

    private int serverPort;
    private ServerSocket serverSocket;
    private boolean isStopped;
    private Thread runningThread;

    MultiThreadedServer(int serverPort) {
      this.serverPort = serverPort;
    }

    public void run() {
      synchronized (this) {
        this.runningThread = Thread.currentThread();
      }

      openServerSocket();

      while (!isStopped) {
        Socket clientSocket;
        try {
          clientSocket = serverSocket.accept();
        } catch (IOException e) {
          if (isStopped) {
            System.out.println("Server stopped");
            return;
          }
          throw new RuntimeException("Error accepting client connection");
        }

        // connection accepted
        if (clientSocket == null) {
          System.out.println("Failed to accept client socket");
        } else {
          new Thread(new WorkerRunnable(clientSocket)).start();
        }
      }

      System.out.println("Server stopped");
    }

    public synchronized boolean isStopped() {
      return isStopped;
    }

    public synchronized void stop() {
      isStopped = true;
      try {
        serverSocket.close();
      } catch (IOException e) {
        throw new RuntimeException("Error closing server socket", e);
      }
    }

    private void openServerSocket() {
      try {
        serverSocket = new ServerSocket(serverPort);
        serverSocket.setReuseAddress(true);
      } catch (IOException e) {
        throw new RuntimeException("Error opening server socket", e);
      }
      System.out.println(String.format("Stream server started on %d", serverPort));
    }
  }

  private static class WorkerRunnable implements Runnable {

    private final Socket clientSocket;
    private final Agent agent;

    WorkerRunnable(final Socket clientSocket) {
      this.clientSocket = clientSocket;
      this.agent = Agent.createBasicServer();
    }

    private void closeClient() {
      if (clientSocket.isClosed()) {
        return;
      }

      try {
        clientSocket.close();
        System.out.println("Closed client");
      } catch(IOException e) {
        System.out.println("Error closing client");
        e.printStackTrace();
      }
    }

    public void run() {
      try {
        DataInputStream inFromClient = new DataInputStream(clientSocket.getInputStream());
        final DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());

        ResponseHandler rh = new ResponseHandler() {
          @Override
          public void onQuest(byte[] messageData, InetAddress destAddress, int destPort) {
            System.out.println(String.format("Received response: %d bytes", messageData.length));

            try {
              outToClient.write(messageData, 0, messageData.length);
              outToClient.flush();
            } catch (IOException e) {
              System.out.println("Error writing response to client");
              e.printStackTrace();
              closeClient();
            }
          }

          @Override
            public void onResponse(byte[] messageData, InetAddress destAddress, int destPort) {
              System.out.println(String.format("Received response: %d bytes", messageData.length));

              try {
                outToClient.write(messageData, 0, messageData.length);
                outToClient.flush();
              } catch (IOException e) {
                System.out.println("Error writing response to client");
                e.printStackTrace();
                closeClient();
              }
            }

          @Override
          public void onIndication(byte[] messageData, InetAddress destAddress, int destPort) {
            onResponse(messageData, destAddress, destPort);
          }

        };
        InetSocketAddress responseAddress =
            new InetSocketAddress(clientSocket.getInetAddress(), clientSocket.getPort());

        byte[] buffer = new byte[4];
        while (!clientSocket.isClosed()) {
          inFromClient.read(buffer);

          int totalBytes = agent.totalBytesInMessage(buffer);
          byte[] msgBuffer = new byte[totalBytes];
          System.arraycopy(buffer, 0, msgBuffer, 0, buffer.length);
          if (totalBytes > buffer.length) {
            inFromClient.read(msgBuffer, 4, totalBytes - 4);
          }

          System.out.println(String.format("Received new message: %d bytes from %s %d", msgBuffer.length, clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort()));
          agent.onMessage(msgBuffer, responseAddress, rh);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (!clientSocket.isClosed()) {
          closeClient();
        }
      }
    }
  }
}
