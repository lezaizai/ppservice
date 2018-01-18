/*
 * This file is part of JSTUN. 
 * 
 * Copyright (c) 2005 Thomas King <king@t-king.de> - All rights
 * reserved.
 * 
 * This software is licensed under either the GNU Public License (GPL),
 * or the Apache 2.0 license. Copies of both license agreements are
 * included in this distribution.
 */

package io.vos.stun.util;

import java.util.*;
import java.net.*;

public class Address {
	int firstOctet;
	int secondOctet;
	int thirdOctet;
	int fourthOctet;
	
	public Address(int firstOctet, int secondOctet, int thirdOctet, int fourthOctet) {
		this.firstOctet = firstOctet;
		this.secondOctet = secondOctet;
		this.thirdOctet = thirdOctet;
		this.fourthOctet = fourthOctet;
	}
	
	public Address(String address) {
		StringTokenizer st = new StringTokenizer(address, ".");
		int i = 0;
		while (st.hasMoreTokens()) {
			int temp = Integer.parseInt(st.nextToken());
			switch (i) {
			case 0: firstOctet = temp; ++i; break;
			case 1: secondOctet = temp; ++i; break;
			case 2: thirdOctet = temp; ++i; break;
			case 3: fourthOctet = temp; ++i; break;
			}
		}
	}
	
	public Address(byte[] address) {
		firstOctet = Bytes.byteToInt(address[0]);
		secondOctet = Bytes.byteToInt(address[1]);
		thirdOctet = Bytes.byteToInt(address[2]);
		fourthOctet = Bytes.byteToInt(address[3]);
	}
	
	public String toString() {
		return firstOctet + "." + secondOctet + "." + thirdOctet + "." + fourthOctet;
	}
	
	public byte[] getBytes() {
		byte[] result = new byte[4];
		result[0] = Bytes.intToBytes(firstOctet, 1)[0];
		result[1] = Bytes.intToBytes(secondOctet, 1)[0];
		result[2] = Bytes.intToBytes(thirdOctet, 1)[0];
		result[3] = Bytes.intToBytes(fourthOctet, 1)[0];
		return result;
	}
	
	public InetAddress getInetAddress() throws UnknownHostException {
		byte[] address = new byte[4];
		address[0] = Bytes.intToBytes(firstOctet, 1)[0];
		address[1] = Bytes.intToBytes(secondOctet, 1)[0];
		address[2] = Bytes.intToBytes(thirdOctet, 1)[0];
		address[3] = Bytes.intToBytes(fourthOctet, 1)[0];
		return InetAddress.getByAddress(address);
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		byte[] data1 = this.getBytes();
		byte[] data2 = ((Address) obj).getBytes();
		if ((data1[0] == data2[0]) && (data1[1] == data2[1]) &&
				(data1[2] == data2[2]) && (data1[3] == data2[3])) return true;
		return false;
	}
	
	public int hashCode() {
		return (firstOctet << 24) + (secondOctet << 16) + (thirdOctet << 8) + fourthOctet; 
	}

}
