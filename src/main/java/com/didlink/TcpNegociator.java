package com.didlink;

import io.vos.stun.demo.TcpEstablishedListener;
import io.vos.stun.demo.TcpStunClient;
import io.vos.stun.ice.Peer;
import io.vos.stun.protocol.Agent;

import java.io.*;
import java.net.*;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by wuh56 on 4/24/2017.
 */
public class TcpNegociator {
//    String stunServer = "www.disneyfans.cn";
//    int stunPort = 3479;
    int timeout = 1000;

    NegociationObserver negociationObserver = new NegociationObserver() {
        @Override
        public void onSuccess(String remoteAddress, int remotePort) {
            System.out.println(String.format("Negociation Succeed!!! %s %d.", remoteAddress, remotePort));

            SendThread sendthread = new SendThread();
            new Thread(sendthread).start();
        }

        @Override
        public void onSuccess(Peer peer) {
            String remoteAddress = peer.getAddress();
            int remotePort = peer.getPort();
            if (!AppSingleton.getInstance().getNodeCollection().hasPeer(remoteAddress, remotePort)) {
                AppSingleton.getInstance().getNodeCollection().addPeer(remoteAddress, remotePort);
            }
            AppSingleton.getInstance().setSendService( createSendService(peer.getSocket()));

            ReceiveThread receiveThread = new ReceiveThread(peer.getSocket());

            SendThread sendthread = new SendThread();

            ExecutorService executor = Executors.newFixedThreadPool(2);
            executor.submit(new Thread(receiveThread));
            executor.submit(new Thread(sendthread));
            executor.shutdown();

        }

        @Override
        public void onError(String remoteAddress, int remotePort) {
        }

        @Override
        public void onTimeout(String remoteAddress, int remotePort) {

        }

    };

    public void start() throws IOException {

        TcpEstablishedListener tcpEstablishedListener = new TcpEstablishedListener() {
            @Override
            public void established(String publicAddress, int publicPort, int localPort) {

                System.out.println(String.format("TcpEstablishedListener public address %s %d, local port %d", publicAddress, publicPort, localPort));

                try {
                    String localIP = InetAddress.getLocalHost().getHostAddress();
                    System.out.println(String.format("Local IP address: %s",localIP));

                    BufferedReader stdin;
                    stdin = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("Input remote ip and port:");
                    String remotestr = stdin.readLine();
                    String remoteAddress = remotestr.split(" ")[0];
                    int remotePort = Integer.valueOf(remotestr.split(" ")[1]);

                    System.out.println(remoteAddress + " " + remotePort);

                    AcceptThread acceptThread = new AcceptThread(remoteAddress, remotePort, localPort, true, negociationObserver);

                    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                    executor.schedule(acceptThread, 10, TimeUnit.MILLISECONDS);
                    executor.shutdown();

//                    NegociationTask task = new NegociationTask(stunServer, stunPort, timeout,
//                            publicAddress, publicPort,
//                            remoteAddress, remotePort,
//                            localPort,
//                            negociationObserver);
//
//                    ScheduledExecutorService executor1 = Executors.newScheduledThreadPool(1);
//                    executor1.schedule(task, 10, TimeUnit.MILLISECONDS);
//                    executor1.shutdown();

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

        TcpStunClient tcpStunClient = new TcpStunClient();
        tcpStunClient.tryTest(AppSingleton.stunServer, AppSingleton.stunPort, tcpEstablishedListener);

    }


    private boolean tryConnect(String remoteAddress, int remotePort,int localPort, int waittime, NegociationObserver negociationObserver) {
        System.out.println("tryConnect...timeout: " + waittime);
        boolean isError = false;
        Socket socket = null;
        try {
            socket = new Socket();
            socket.setReuseAddress(true);
            socket.setSoTimeout(waittime);
            socket.bind(new InetSocketAddress(localPort));
            socket.connect(new InetSocketAddress(remoteAddress, remotePort),waittime);
            System.out.println("tryConnect...succeed");
            if (negociationObserver != null) {
                Peer peer = new Peer(remoteAddress, remotePort, false);
                peer.setSocket(socket);
                negociationObserver.onSuccess(peer);
            }
        } catch (SocketException e) {
            e.printStackTrace();
            isError = true;
        } catch (IOException e) {
            e.printStackTrace();
            isError = true;
        } finally {
        }
        if (isError) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
        }
        return !isError;
    }

    private boolean tryAccept(int localPort, int waittime, NegociationObserver negociationObserver) {
        System.out.println("tryAccept...timeout: " + waittime);
        ServerSocket serverSocket = null;
        boolean isError = false;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(waittime);
            serverSocket.bind(new InetSocketAddress(localPort));

            while (true) {
                Socket clientSocket;
                clientSocket = serverSocket.accept();
                System.out.println("Accepted remote connection.");
                String remoteAddress = clientSocket.getRemoteSocketAddress().toString();
                int remotePort = clientSocket.getPort();
                if (negociationObserver != null) {
                    Peer peer = new Peer(remoteAddress, remotePort, false);
                    peer.setSocket(clientSocket);
                    negociationObserver.onSuccess(peer);
                }
                //clientSocket.close();
                break;
            }

        } catch (SocketException e) {
            e.printStackTrace();
            isError = true;
        } catch (IOException e) {
            e.printStackTrace();
            isError = true;
        } finally {
        }

        if (isError) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
        }
        return !isError;
    }

    private class AcceptThread extends TimerTask {

        private String remoteAddress;
        private int remotePort;
        private int localPort;
        private boolean isAccept;
        NegociationObserver negociationObserver;

        public void run() {
            System.out.println("Ready accept.");

            boolean result = false;
            if (isAccept) {
                result = tryAccept(localPort, (int)(Math.random()*500 + 1500), negociationObserver);
            } else {
                result = tryConnect(remoteAddress, remotePort, localPort, (int)(Math.random()*500 + 1500), negociationObserver);
            }

            if (result) {
                System.out.println("Succeed, congratulation!!");
            } else {
                AcceptThread acceptThread = new AcceptThread(remoteAddress, remotePort, localPort,!isAccept,negociationObserver);
                ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                executor.schedule(acceptThread, 10, TimeUnit.MILLISECONDS);
                executor.shutdown();
            }

        }

        AcceptThread(String remoteAddress, int remotePort, int localPort, boolean isAccept, NegociationObserver negociationObserver) {
            this.remoteAddress = remoteAddress;
            this.remotePort = remotePort;
            this.localPort = localPort;
            this.isAccept = isAccept;
            this.negociationObserver = negociationObserver;
        }
    }

    private class ReceiveThread implements Runnable {

        Socket socket;
        Agent agent;

        public void run() {
            System.out.println("Ready receiving.");
            try {
                socket.setSoTimeout(0);
            } catch (SocketException e) {
                e.printStackTrace();
                return;
            }

            boolean repeat = false;
            byte[] buffer = new byte[4];

            try {
                DataInputStream inFromServer = new DataInputStream(socket.getInputStream());

                while (!socket.isClosed()) {
                    inFromServer.readFully(buffer);

                    int totalBytes = agent.totalBytesInMessage(buffer);
                    byte[] msgBuffer = new byte[totalBytes];
                    System.arraycopy(buffer, 0, msgBuffer, 0, buffer.length);
                    if (totalBytes > buffer.length) {
                        inFromServer.read(msgBuffer, 4, totalBytes - 4);
                    }
                    String recStr = new String(msgBuffer, 0, msgBuffer.length);
                    System.out.println(String.format("%s %d: %s",socket.getInetAddress(), socket.getPort(), recStr));

                 }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        ReceiveThread(Socket socket) {
            this.socket = socket;
            this.agent = Agent.createBasicServer();
        }
    }

    private SendService createSendService(
            final Socket socket) {
        return new SendService() {
            @Override
            public void sendMessage(String msg, String remoteAddr, int remotePort) {
            }
            @Override
            public void sendMessage(byte[] msgBytes, String remoteAddr, int remotePort) {
            }

            @Override
            public void sendTcpMessage(String msg) {
                try {
                    final DataOutputStream outToRemote = new DataOutputStream(socket.getOutputStream());
                    byte[] buf = msg.getBytes();
                    outToRemote.write(buf, 0, buf.length);
                    outToRemote.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void sendTcpMessage(byte[] msgBytes) {
                try {
                    final DataOutputStream outToRemote = new DataOutputStream(socket.getOutputStream());
                    outToRemote.write(msgBytes, 0, msgBytes.length);
                    outToRemote.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private static class SendThread implements Runnable {

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
                    AppSingleton.getInstance().getSendService().sendTcpMessage(line);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        SendThread() {
       }
    }
}
