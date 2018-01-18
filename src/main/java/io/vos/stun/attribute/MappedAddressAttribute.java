package io.vos.stun.attribute;

import static io.vos.stun.attribute.Attributes.*;

import io.vos.stun.util.Bytes;

import com.google.common.base.Preconditions;

/**
 * @see //https://www.iana.org/assignments/stun-parameters/stun-parameters.txt
 *
 * The MAPPED-ADDRESS attribute indicates a reflexive transport address
 * of the client.  It consists of an 8-bit address family and a 16-bit
 * port, followed by a fixed-length value representing the IP address.
 * If the address family is IPv4, the address MUST be 32 bits.  If the
 * address family is IPv6, the address MUST be 128 bits.  All fields
 * must be in network byte order.
 *
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |0 0 0 0 0 0 0 0|    Family     |           Port                |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                                                               |
 *    |                 Address (32 bits or 128 bits)                 |
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 *             Figure 5: Format of MAPPED-ADDRESS Attribute
 */
public final class MappedAddressAttribute extends BaseAttribute {

  /** MAPPED_ADDRESS Address Family */
  public static final byte AF_IPV4 = 0x01;
  public static final byte AF_IPV6 = 0x02;

  private final byte addressFamily;
  private final int port;
  private final byte[] mappedAddress;

  MappedAddressAttribute(int type, int length, byte[] valueData) {
    super(type, length, valueData);

    addressFamily = valueData[1];
    port = Bytes.twoBytesToInt(valueData[2], valueData[3]);
    mappedAddress = new byte[mappedAddressByteLength()];
    System.arraycopy(valueData, 4, mappedAddress, 0, mappedAddressByteLength());

    //System.out.println("MappedAddressAttribute");
    //System.out.println(new Address(mappedAddress).toString());

    //System.out.println(String.format("MappedAddressAttribute Address: %s %d", new Address(mappedAddress).toString(), port));
  }

  @Override
  protected void validateInternal(int type, int length, byte[] valueData) {
    int wouldBeAddressFamily = valueData[1];
    Preconditions.checkState(
        wouldBeAddressFamily == AF_IPV4 || wouldBeAddressFamily == AF_IPV6,
        "Invalid address family value " + wouldBeAddressFamily);

    int expectedByteLength = wouldBeAddressFamily == AF_IPV4 ? 8 : 20;
    Preconditions.checkState(valueData.length == expectedByteLength, String.format(
        "Invalid value data length %d, expected %d", valueData.length, expectedByteLength));
  }

  private int mappedAddressByteLength() {
    return isIPv4() ? 4 : 16;
  }

  public boolean isXorMapped() {
    return getType() == ATTRIBUTE_XOR_MAPPED_ADDRESS;
  }

  public int getPort() {
    return port;
  }

  public byte[] getMappedAddress() {
    return mappedAddress;
  }

  /**
   * Whether this MappedAddress is an IPv4 address. False indicates IPv6.
   */
  public  boolean isIPv4() {
    return addressFamily == AF_IPV4;
  }

  public static MappedAddressAttribute createAttribute(
      int addressFamily, byte[] port, byte[] address, boolean isXorMapped) {

    //System.out.println(String.format("MappedAddressAttribute address attribute: %d", addressFamily));
    //System.out.println(String.format("MappedAddressAttribute address attribute: %s %d", new Address(address).toString(), Bytes.twoBytesToInt(port[0],port[1])));

    Preconditions.checkArgument(addressFamily == AF_IPV4 || addressFamily == AF_IPV6);
    Preconditions.checkArgument(port.length == 2);
    if (addressFamily == AF_IPV4) {
      Preconditions.checkArgument(address.length == 4);
    } else {
      Preconditions.checkArgument(address.length == 16);
    }

    byte[] valueData = Bytes.concat(
        Bytes.intToBytes(addressFamily, 2 /* maxBytes */),
        port,
        address);
    return new MappedAddressAttribute(
        isXorMapped ? ATTRIBUTE_XOR_MAPPED_ADDRESS : ATTRIBUTE_MAPPED_ADDRESS,
        valueData.length,
        Bytes.padTo4ByteBoundary(valueData));
  }
}
