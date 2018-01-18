package com.didlink;

import com.google.common.base.Preconditions;
import io.vos.stun.attribute.*;
import io.vos.stun.demo.UdpStunClient;
import io.vos.stun.demo.UdpEstablishedListener;
import io.vos.stun.ice.Peer;
import io.vos.stun.message.Message;
import io.vos.stun.protocol.Agent;
import io.vos.stun.protocol.ResponseHandler;
import io.vos.stun.util.Address;
import io.vos.stun.util.Bytes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.vos.stun.message.Messages.*;

/**
 * Created by wuh56 on 4/24/2017.
 */
public class UdpNegociator {
//    String stunServer = "www.disneyfans.cn";
//    int stunPort = 3478;
    int timeout = 500;
//    Timer timer;

    NegociationObserver negociationObserver = new NegociationObserver() {
        @Override
        public void onSuccess(String remoteAddress, int remotePort) {
            System.out.println("Negociation Succeed!!!");
            SendThread sendthread = new SendThread(remoteAddress, remotePort);
            new Thread(sendthread).start();
        }

        @Override
        public void onSuccess(Peer peer) {

        }

        @Override
        public void onError(String remoteAddress, int remotePort) {
        }

        @Override
        public void onTimeout(String remoteAddress, int remotePort) {

        }

    };

    public void start() throws IOException {
//        timer = new Timer(true);

        UdpEstablishedListener udpEstablishedListener = new UdpEstablishedListener() {
            @Override
            public void established(DatagramSocket dgramSocket, String publicAddress, int publicPort, int localPort) {

                System.out.println(String.format("UdpEstablishedListener public address %s %d, local port %d", publicAddress, publicPort, localPort));

                try {
                    startListen(dgramSocket, publicAddress, publicPort, localPort);

                    BufferedReader stdin;
                    stdin = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("Input remote ip and port:");
                    String remotestr = stdin.readLine();
                    String remoteAddress = remotestr.split(" ")[0];
                    int remotePort = Integer.valueOf(remotestr.split(" ")[1]);

                    System.out.println("try to connect: " + remoteAddress + " " + remotePort);

                    NegociationTask task = new NegociationTask(AppSingleton.stunServer, AppSingleton.stunPort, timeout,
                            publicAddress, publicPort,
                            remoteAddress, remotePort,
                            negociationObserver);
                    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                    executor.schedule(task, 10, TimeUnit.MILLISECONDS);
                    executor.shutdown();

                    //timer.schedule(task, 10);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

            }

            @Override
            public void onError() {
            }

        };

        String localIP = InetAddress.getLocalHost().getHostAddress();
        System.out.println(String.format("Local IP address: %s",localIP));

        UdpStunClient udpStunClient = new UdpStunClient();
        udpStunClient.tryTest(AppSingleton.stunServer, AppSingleton.stunPort, udpEstablishedListener);

    }


//    private void testChat(String publicAddress, int publicPort, int localPort) {
//        try {
//            DatagramSocket dgramSocket = new DatagramSocket(localPort);
//            AppSingleton.getInstance().setSendService( createSendService(dgramSocket));
//
//            ReceiveThread receiveThread = new ReceiveThread(dgramSocket, publicAddress, publicPort);
//
//            SendThread sendthread = new SendThread(publicAddress, publicPort, dgramSocket);
//
//            new Thread(receiveThread).start();
//            new Thread(sendthread).start();
//
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
//
//    }

    private void startListen(DatagramSocket dgramSocket, String publicAddress, int publicPort, int localPort) throws SocketException {
//        DatagramSocket dgramSocket = new DatagramSocket(localPort);
        AppSingleton.getInstance().setDgramSocket(dgramSocket);
        AppSingleton.getInstance().setSendService( createSendService(dgramSocket));

        ReceiveThread receiveThread = new ReceiveThread(dgramSocket, publicAddress, publicPort);
        new Thread(receiveThread).start();
    }

    private class NegociationTask extends TimerTask {
        private String stunServer;
        private int stunPort;
        private int timeout;
        private String publicAddress;
        private int publicPort;
        private String remoteAddress;
        private int remotePort;
        NegociationObserver negociationObserver;

        NegociationTask(String stunServer,
                        int stunPort,
                        int timeout,
                        String publicAddress,
                        int publicPort,
                        String remoteAddress,
                        int remotePort,
                        NegociationObserver negociationObserver) {
            super();
            this.stunServer = stunServer;
            this.stunPort = stunPort;
            this.timeout = timeout;
            this.remotePort = remotePort;
            this.remoteAddress = remoteAddress;
            this.publicAddress = publicAddress;
            this.publicPort = publicPort;
            this.negociationObserver = negociationObserver;
        }

        public void run() {
            if (!AppSingleton.getInstance().getNodeCollection().hasPeer(remoteAddress, remotePort)) {
                AppSingleton.getInstance().getNodeCollection().addPeer(remoteAddress, remotePort);
            }

            if (!AppSingleton.getInstance().getNodeCollection().getFirstPeer(remoteAddress, remotePort).getRequestReceived()) {
                byte[] attributesBytes = makeMappedAttrbytes(remoteAddress, remotePort);
                Message request = Message.builder()
                        .setMessageClass(MESSAGE_CLASS_INDICATION)
                        .setMessageMethod(MESSAGE_METHOD_NEGOCIATE)
                        .generateTransactionID()
                        .setAttributeBytes(attributesBytes)
                        .build();

                byte[] requestBytes = request.getBytes();
                AppSingleton.getInstance().getSendService().sendMessage(requestBytes, stunServer, stunPort);
            }

            if (!AppSingleton.getInstance().getNodeCollection().getFirstPeer(remoteAddress, remotePort).getResponseReceived()) {
                byte[] attributesBytes1 = makeMappedAttrbytes(publicAddress, publicPort);
                Message request1 = Message.builder()
                        .setMessageClass(MESSAGE_CLASS_REQUEST)
                        .setMessageMethod(MESSAGE_METHOD_NEGOCIATE)
                        .generateTransactionID()
                        .setAttributeBytes(attributesBytes1)
                        .build();

                byte[] requestBytes1 = request1.getBytes();
                AppSingleton.getInstance().getSendService().sendMessage(requestBytes1, remoteAddress, remotePort);
            }


            try {
                Thread.sleep(500);
                if (AppSingleton.getInstance().getNodeCollection().getFirstPeer(remoteAddress, remotePort).isSucceed()) {
                    if (negociationObserver != null) {
                        negociationObserver.onSuccess(remoteAddress, remotePort);
                    }
                } else if (AppSingleton.getInstance().getNodeCollection().getFirstPeer(remoteAddress, remotePort).isTimeout()) {
                    if (negociationObserver != null) {
                        negociationObserver.onTimeout(remoteAddress, remotePort);
                    }
                } else {
                    NegociationTask task = new NegociationTask(stunServer, stunPort, timeout,
                            publicAddress, publicPort,
                            remoteAddress, remotePort,
                            negociationObserver);
                    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                    executor.schedule(task, 10, TimeUnit.MILLISECONDS);
                    executor.shutdown();
                    //timer.schedule(task, 10);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private class ReceiveThread implements Runnable {

        DatagramSocket dgramSocket;
        String publicAddress;
        int publicPort;
        Agent agent;
        SendService sendService = AppSingleton.getInstance().getSendService();

        public void run() {
            System.out.println("Ready receiving.");
            try {
                dgramSocket.setSoTimeout(0);
            } catch (SocketException e) {
                e.printStackTrace();
                return;
            }

            boolean repeat = false;
            while (true) {
                try {

                    byte[] buffer = new byte[1024];
                    DatagramPacket packet =  new DatagramPacket(buffer, buffer.length);
                    dgramSocket.receive(packet);
                    int packetLen = packet.getLength();
//                    System.out.println(String.format("Received packet of size %d bytes", packetLen));
                    byte[] msgBuffer = new byte[packetLen];
                    System.arraycopy(buffer, 0, msgBuffer, 0, packetLen);

                    int packageType = msgBuffer[0] >>> 6;

                    Message message = new Message(Preconditions.checkNotNull(msgBuffer));

                    final InetSocketAddress remoteAddress =
                            new InetSocketAddress(packet.getAddress(), packet.getPort());
                    switch (message.getMessageMethod()) {
                        case MESSAGE_METHOD_NEGOCIATE:
                            ResponseHandler rh =
                                    createMessageHandler(dgramSocket, publicAddress, publicPort);
                            switch (message.getMessageClass()) {
                                case MESSAGE_CLASS_INDICATION:
                                    agent.onMessage(msgBuffer, remoteAddress, rh);
                                    break;
                                case MESSAGE_CLASS_REQUEST:
                                    agent.onMessage(msgBuffer, remoteAddress, rh);
                                    break;
                                case MESSAGE_CLASS_RESPONSE:
                                    agent.onMessage(msgBuffer, remoteAddress, rh);
                                    break;
                                default:
                                    throw new AssertionError("Handling invalid message class, this should have been validated");
                            }
                            break;
                        case MESSAGE_METHOD_TRANSFER_FILE:
                            ResponseHandler fth =
                                createFileTransferHandler(dgramSocket, publicAddress, publicPort);
                            switch (message.getMessageClass()) {
                                case MESSAGE_CLASS_REQUEST:
                                    agent.onMessage(msgBuffer, remoteAddress, fth);
                                    break;
                                case MESSAGE_CLASS_RESPONSE:
                                    agent.onMessage(msgBuffer, remoteAddress, fth);
                                    break;
                                case MESSAGE_CLASS_INDICATION:
                                    agent.onMessage(msgBuffer, remoteAddress, fth);
                                    break;
                                default:
                                    throw new AssertionError("Handling invalid message class, this should have been validated");
                            }
                            break;
                        default:
                            String recStr = new String(packet.getData(), 0, packet.getLength());

                            System.out.println(String.format("%s %d: %s",packet.getAddress(), packet.getPort(), recStr));
                            if (!repeat && recStr.equals("repeat") && this.sendService != null) {
                                this.sendService.sendMessage(recStr, packet.getAddress().getHostAddress(), packet.getPort());
                                repeat = true;
                            }
//                            throw new AssertionError("Handling invalid message class, this should have been validated");
                    }

                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        ReceiveThread(DatagramSocket dgramSocket, String publicAddress, int publicPort) {
            this.dgramSocket = dgramSocket;
            this.publicAddress = publicAddress;
            this.publicPort = publicPort;
            this.agent = Agent.createBasicServer();
        }
    }

    private SendService createSendService(
            final DatagramSocket dgramSocket) {
        return new SendService() {
            @Override
            public void sendMessage(String msg, String remoteAddr, int remotePort) {
                System.out.println(String.format("Send UPD message to %s %d.", remoteAddr, remotePort));
                try {
                    byte[] buf = msg.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    packet.setSocketAddress( new InetSocketAddress(remoteAddr, remotePort) );
                    dgramSocket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void sendMessage(byte[] msgBytes, String remoteAddr, int remotePort) {
                System.out.println(String.format("Send UPD message to %s %d.", remoteAddr, remotePort));
                try {
                    DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length);
                    packet.setSocketAddress( new InetSocketAddress(remoteAddr, remotePort) );
                    dgramSocket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void sendTcpMessage(String msg) {

            }

            @Override
            public void sendTcpMessage(byte[] msgBytes) {

            }
        };
    }

    private static class SendThread implements Runnable {

        String remoteAddress;
        int remotePort;

        public void run() {
            System.out.println("Ready sending.");
            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String line;
                // 从键盘读取
                while ((line = reader.readLine()) != null)
                {
                    line = line.trim();
                    if (line.length() == 0)
                    {
                        break;
                    }
                    //AppSingleton.getInstance().getSendService().sendMessage(line, remoteAddress, remotePort);

                    Attribute attribute = FileInfoAttribute
                            .createAttribute(66666, line.getBytes());
                    AttributesCollection attributes = AttributesCollection.EMPTY_COLLECTION;
                    byte[] attributeBytes = attributes.replyBuilder()
                            .addAttribute(attribute)
                            .build()
                            .toByteArray();

                    Message request = Message.builder()
                            .setMessageClass(MESSAGE_CLASS_REQUEST)
                            .setMessageMethod(MESSAGE_METHOD_TRANSFER_FILE)
                            .generateTransactionID()
                            .setAttributeBytes(attributeBytes)
                            .build();
                    byte[] requestBytes = request.getBytes();

                    AppSingleton.getInstance().getSendService().sendMessage(requestBytes, remoteAddress, remotePort);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        SendThread(String remoteAddress, int remotePort) {
            this.remoteAddress = remoteAddress;
            this.remotePort = remotePort;
        }
    }

    private ResponseHandler createMessageHandler(
            final DatagramSocket dgramSocket, final String publicAddress, final int publicPort) {
        return new ResponseHandler() {
            @Override
            public void onQuest(byte[] messageData, InetAddress destAddress, int destPort) {
                System.out.println(String.format("Received request from %s %d", destAddress.getHostAddress(), destPort));

                byte[] attributesBytes = makeMappedAttrbytes(publicAddress, publicPort);
                Message request = Message.builder()
                        .setMessageClass(MESSAGE_CLASS_RESPONSE)
                        .setMessageMethod(MESSAGE_METHOD_NEGOCIATE)
                        .generateTransactionID()
                        .setAttributeBytes(attributesBytes)
                        .build();

                byte[] requestBytes = request.getBytes();
                AppSingleton.getInstance().getSendService().sendMessage(requestBytes, destAddress.getHostAddress(), destPort);
                AppSingleton.getInstance().getNodeCollection().getFirstPeer(destAddress, destPort).setRequestReceived();
            }

            @Override
            public void onResponse(byte[] messageData, InetAddress destAddress, int destPort) {
                System.out.println(String.format("Received response from %s %d", destAddress.getHostAddress(), destPort));

                System.out.println(String.format("ResponseReceived %b", AppSingleton.getInstance().getNodeCollection().getFirstPeer(destAddress, destPort).getResponseReceived()));
                AppSingleton.getInstance().getNodeCollection().getFirstPeer(destAddress, destPort).setResponseReceived();
                System.out.println(String.format("ResponseReceived %b", AppSingleton.getInstance().getNodeCollection().getFirstPeer(destAddress, destPort).getResponseReceived()));
            }

            @Override
            public void onIndication(byte[] messageData, InetAddress destAddress, int destPort) {

                Message message = new Message(Preconditions.checkNotNull(messageData));
                byte[] attributesBytes = makeMappedAttrbytes(publicAddress, publicPort);

                Message request = Message.builder()
                        .setMessageClass(MESSAGE_CLASS_REQUEST)
                        .setMessageMethod(MESSAGE_METHOD_NEGOCIATE)
                        .setTransactionId(message.getTransactionId())
                        .setAttributeBytes(attributesBytes)
                        .build();
                byte[] requestBytes = request.getBytes();

                AppSingleton.getInstance().getSendService().sendMessage(requestBytes, destAddress.getHostAddress(), destPort);

                if (!AppSingleton.getInstance().getNodeCollection().hasPeer(destAddress, destPort)) {
                    AppSingleton.getInstance().getNodeCollection().addPeer(destAddress.getHostAddress(), destPort);
                }
            }

        };
    }

    private ResponseHandler createFileTransferHandler(
            final DatagramSocket dgramSocket, final String publicAddress, final int publicPort) {
        return new ResponseHandler() {
            @Override
            public void onQuest(byte[] messageData, InetAddress destAddress, int destPort) {
                System.out.println(String.format("Received request from %s %d", destAddress.getHostAddress(), destPort));

                byte[] attributesBytes = makeMappedAttrbytes(publicAddress, publicPort);
                Message request = Message.builder()
                        .setMessageClass(MESSAGE_CLASS_RESPONSE)
                        .setMessageMethod(MESSAGE_METHOD_NEGOCIATE)
                        .generateTransactionID()
                        .setAttributeBytes(attributesBytes)
                        .build();

                byte[] requestBytes = request.getBytes();
                AppSingleton.getInstance().getSendService().sendMessage(requestBytes, destAddress.getHostAddress(), destPort);
                AppSingleton.getInstance().getNodeCollection().getFirstPeer(destAddress, destPort).setRequestReceived();
            }

            @Override
            public void onResponse(byte[] messageData, InetAddress destAddress, int destPort) {
                System.out.println(String.format("Received response from %s %d", destAddress.getHostAddress(), destPort));

                System.out.println(String.format("ResponseReceived %b", AppSingleton.getInstance().getNodeCollection().getFirstPeer(destAddress, destPort).getResponseReceived()));
                AppSingleton.getInstance().getNodeCollection().getFirstPeer(destAddress, destPort).setResponseReceived();
                System.out.println(String.format("ResponseReceived %b", AppSingleton.getInstance().getNodeCollection().getFirstPeer(destAddress, destPort).getResponseReceived()));
            }

            @Override
            public void onIndication(byte[] messageData, InetAddress destAddress, int destPort) {

                Message message = new Message(Preconditions.checkNotNull(messageData));
                byte[] attributesBytes = makeMappedAttrbytes(publicAddress, publicPort);

                Message request = Message.builder()
                        .setMessageClass(MESSAGE_CLASS_REQUEST)
                        .setMessageMethod(MESSAGE_METHOD_NEGOCIATE)
                        .setTransactionId(message.getTransactionId())
                        .setAttributeBytes(attributesBytes)
                        .build();
                byte[] requestBytes = request.getBytes();

                AppSingleton.getInstance().getSendService().sendMessage(requestBytes, destAddress.getHostAddress(), destPort);

                if (!AppSingleton.getInstance().getNodeCollection().hasPeer(destAddress, destPort)) {
                    AppSingleton.getInstance().getNodeCollection().addPeer(destAddress.getHostAddress(), destPort);
                }
            }

        };
    }

    private byte[] makeMappedAttrbytes(String address, int port) {
        byte addressFamily = MappedAddressAttribute.AF_IPV4;
        byte[] addressBytes = (new Address(address)).getBytes();
        byte[] magicCookieBytes = Bytes.intToBytes(MAGIC_COOKIE_FIXED_VALUE);
        byte[] portBytes = Bytes.intToBytes(port);
        byte[] xPortBytes = new byte[]{
                (byte) (portBytes[2] ^ magicCookieBytes[0]),
                (byte) (portBytes[3] ^ magicCookieBytes[1])
        };

        byte[] xAddressBytes = new byte[addressBytes.length];
        byte[] xorBytes = magicCookieBytes;

        for (int i = 0; i < addressBytes.length; i++) {
            xAddressBytes[i] = (byte) (addressBytes[i] ^ xorBytes[i]);
        }

        Attribute attribute = MappedAddressAttribute
                .createAttribute(addressFamily, xPortBytes, xAddressBytes, true /* isXor */);
        AttributesCollection attributes = AttributesCollection.EMPTY_COLLECTION;
        return attributes.replyBuilder()
                .addAttribute(attribute)
                .build()
                .toByteArray();
    }
}
