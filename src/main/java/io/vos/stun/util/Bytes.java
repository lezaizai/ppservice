package io.vos.stun.util;

public final class Bytes {

  /**
   * Returns a 4-byte array representation of the int in network byte order (big
   * endian).
   */
  public static byte[] intToBytes(int value) {
    return intToBytes(value, 4);
  }

  /**
   * Returns the given int converted to the least significant {@code maxBytes}
   * length byte array, in network byte order.
   */
  public static byte[] intToBytes(int value, int maxBytes) {
    if (maxBytes == 1) {
      return new byte[] { (byte)(value & 0xff) };
    } else if (maxBytes == 2) {
      return new byte[] {
        (byte)((value >>> 8) & 0xff),
        (byte)(value & 0xff)
      };
    } else if (maxBytes == 3) {
      return new byte[] {
        (byte)((value >>> 16) & 0xff),
        (byte)((value >>> 8) & 0xff),
        (byte)(value & 0xff)
      };
    } else if (maxBytes == 4) {
      return new byte[] {
        (byte)((value >>> 24) & 0xff),
        (byte)((value >>> 16) & 0xff),
        (byte)((value >>> 8) & 0xff),
        (byte)(value & 0xff)
      };
    } else {
      throw new IllegalArgumentException("Invalid max byte value " + maxBytes);
    }
  }

  public static int byteToInt(byte value) {
    return value & 0xff;
  }

  public static int twoBytesToInt(byte byte1, byte byte2) {
    return ((byte1 & 0xff) << 8) | byte2 & 0xff;
  }

  public static int fourBytesToInt(
      byte byte1, byte byte2, byte byte3, byte byte4) {
    return (byte1 & 0xff) << 24 |
        (byte2 & 0xff) << 16 |
        (byte3 & 0xff) << 8 |
        byte4 & 0xff;
  }

  public static byte[] long2Bytes(long num) {
    byte[] byteNum = new byte[8];
    for (int ix = 0; ix < 8; ++ix) {
      int offset = 64 - (ix + 1) * 8;
      byteNum[ix] = (byte) ((num >> offset) & 0xff);
    }
    return byteNum;
  }

  public static long bytes2Long(byte[] byteNum) {
    long num = 0;
    for (int ix = 0; ix < 8; ++ix) {
      num <<= 8;
      num |= (byteNum[ix] & 0xff);
    }
    return num;
  }

  public static byte[] padTo4ByteBoundary(byte[] data) {
    int remainder = data.length % 4;
    if (remainder % 4 == 0) {
      return data;
    }
    int padding = 4 - remainder;
    byte[] paddedBuffer = new byte[data.length + padding];
    System.arraycopy(data, 0, paddedBuffer, 0, data.length);
    return paddedBuffer;
  }

  public static byte[] concat(byte[]... byteArrays) {
    int newBytesLength = 0;
    for (int i = 0; i < byteArrays.length; i++) {
      newBytesLength += byteArrays[i].length;
    }

    byte[] newByteArray = new byte[newBytesLength];
    int currentByte = 0;
    for (int i = 0; i < byteArrays.length; i++) {
      System.arraycopy(byteArrays[i], 0, newByteArray, currentByte, byteArrays[i].length);
      currentByte += byteArrays[i].length;
    }
    return newByteArray;
  }
}
