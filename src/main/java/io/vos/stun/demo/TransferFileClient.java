package io.vos.stun.demo;

import io.vos.stun.Exception.BadChecksumException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class TransferFileClient {

	private final int RECV_WINDOW = 5;

	private String fileName;

	private int numPackets;

	private int fileSize;

	/** Timeout in milliseconds */
	private final int TIMEOUT = 5000;

	private int clientPort;

	private int serverPort;

	private InetAddress serverAddr;

	private DatagramSocket clientSocket;

	public TransferFileClient() throws SocketException, IOException {
		promptUser();
		initalizeClient();
		
		if (!establishConnection()) return;

		acceptFile();
	}
	
	@SuppressWarnings("resource")
	private void promptUser() {
		Scanner scan = new Scanner(System.in);
		
		System.out.print("Please specify a port number: ");
		String input = scan.nextLine();
		
		try {
			clientPort = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			System.err.println("Invalid port number");
			promptUser();
		}
		
		System.out.print("Enter server IPv4 address: ");
		String destAddr = scan.nextLine();
		
		try {
			serverAddr = InetAddress.getByName(destAddr);
		} catch (UnknownHostException e) {
			System.err.println("Invalid IPv4 address");
			promptUser();
		}
		
		System.out.print("Enter server port number: ");
		input = scan.nextLine();
		
		try {
			serverPort = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			System.err.println("Invalid port number");
			promptUser();
		}
	}
	
	private void initalizeClient() throws SocketException {
		try {
			clientSocket = new DatagramSocket(clientPort);
			clientSocket.setSoTimeout(TIMEOUT);
		} catch (SocketException e) {
			String message = "Problem starting client on port ";
			message += clientPort;
			message += "\nIs there another instance of this client?";
			throw new SocketException(message);
		}
	}
	
	private boolean establishConnection() throws IOException {
		String msg = "Attempting to connect to server at ";
		msg += serverAddr.getHostAddress();
		msg += " port " + serverPort;
		System.out.println(msg);
		
		System.out.println("Got acknowledgement from server");
		System.out.println(msg + "\n");
		
		return true;
		
	}
	
	private void acceptFile() throws IOException {
		int lastReceived = 0;
		int bytesReceived = 0;
		
		int attempted = 0;
		final int attempts = 3;
		
		final String path = System.getProperty("user.dir") + "/" + fileName;
		
		File file = new File(path);
		
		if (file.exists()) {
			file.delete();
		}
		
		file.createNewFile();
		
		FileOutputStream fos = new FileOutputStream(path, true);
		
		while (lastReceived != numPackets) {
			int recvd = lastReceived;
			
			for (int i = 0; i < RECV_WINDOW; i++) {
				DatagramPacket recvPack = null;

				try {
					recvPack = receive();
				} catch (SocketTimeoutException e) {
					break;
				} catch (BadChecksumException bc) {
					System.err.println(bc.getMessage());
					i--;
					continue;
				}

				if (lastReceived == numPackets) break;				
			}
			
			if (recvd == lastReceived) {
				attempted ++;
			} else {
				attempted = 0;
			}
			
			if (attempted >= attempts) {
				System.err.println("Server not responding.");
				return;
			}

//			DataMessage ackPack = new DataMessage();
//			ackPack.setAckFlag(true);
//			ackPack.setSequenceNum(lastReceived);
//			ackPack.setChecksum();
//
//			send(ackPack.getBytes());
			System.out.println("Sending acknowledgement of packet "
					+ lastReceived + "\n");
		}
		
		System.out.println("File transfer complete.");
		
		fos.close();
	}
	
	private void send(byte[] data) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(data, data.length,
				serverAddr, serverPort);
		
		clientSocket.send(sendPacket);
	}
	
	private DatagramPacket receive() throws IOException, 
		SocketTimeoutException, BadChecksumException {
				
		byte[] recvData = new byte[1024];
		
		DatagramPacket recvPacket = 
				new DatagramPacket(recvData,recvData.length);
		
		clientSocket.receive(recvPacket);

//		int expected = DataMessage.calculateChecksum(recvData);
//		int received = new DataMessage(recvData).getChecksum();

//		if (expected != received)
//			throw new BadChecksumException(expected, received);
		
		return recvPacket;
	}
	
	public static void main(String[] args) {
		try {
			new TransferFileClient();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
