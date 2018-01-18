package io.vos.stun.message;

import static io.vos.stun.message.Messages.*;

import io.vos.stun.util.Bytes;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Objects;

/**
 * @see https://tools.ietf.org/html/rfc5389#section-6
 * 6. STUN Message Structure
 *
 * STUN messages are encoded in binary using network-oriented format
 * (most significant byte or octet first, also commonly known as big-
 * endian).  The transmission order is described in detail in Appendix B
 * of RFC 791 [RFC0791].  Unless otherwise noted, numeric constants are
 * in decimal (base 10).
 *
 * All STUN messages MUST start with a 20-byte header followed by zero
 * or more Attributes.  The STUN header contains a STUN message type,
 * magic cookie, transaction ID, and message length.
 *
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |0 0|     STUN Message Type     |         Message Length        |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Magic Cookie                          |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                                                               |
 *    |                     Transaction ID (96 bits)                  |
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 *                 Figure 2: Format of STUN Message Header
 *
 */
public final class Message {

  private final byte[] data;

  public Message(byte[] data) {
    Preconditions.checkArgument(data.length >= MESSAGE_LEN_HEADER);
    this.data = new byte[data.length];
    System.arraycopy(data, 0, this.data, 0, data.length);
  }

  public static int lengthCheck(byte[] data) {
    Preconditions.checkArgument(data.length >= 4);
    return Bytes.twoBytesToInt(data[2], data[3]) + MESSAGE_LEN_HEADER;
  }

  public byte[] getBytes() {
    byte[] bytes = new byte[data.length];
    System.arraycopy(data, 0, bytes, 0, bytes.length);
    return bytes;
  }

  public byte[] getHeaderBytes() {
    byte[] headerBytes = new byte[MESSAGE_LEN_HEADER];
    System.arraycopy(data, 0, headerBytes, 0, headerBytes.length);
    return headerBytes;
  }

  public byte[] getAttributeBytes() {
    byte[] attrBytes = new byte[this.data.length - MESSAGE_LEN_HEADER];
    System.arraycopy(
        data, MESSAGE_LEN_HEADER, attrBytes, 0, attrBytes.length);
    return attrBytes;
  }

  /**
   * @see https://tools.ietf.org/html/rfc5389#section-6
   *
   * The most significant 2 bits of every STUN message MUST be zeroes.
   * This can be used to differentiate STUN packets from other protocols
   * when STUN is multiplexed with other protocols on the same port.
   */
  public boolean hasNonZeroHeaderBits() {
    return data[0] >>> 6 == 0;
  }

  /**
   * Gets the message class from the header message type, from RFC 5389:
   *
   * Given the following message type:
   *   0011 1110 1110 1111
   *
   * The first two bits are ALWAYS 0. After that all 1's represent method bits
   * and 0's represent class bits.
   *
   * @see https://tools.ietf.org/html/rfc5389#section-6
   *
   * The message type field is decomposed further into the following
   * structure:
   *                      0                 1
   *                      2  3  4 5 6 7 8 9 0 1 2 3 4 5
   *                     +--+--+-+-+-+-+-+-+-+-+-+-+-+-+
   *                     |M |M |M|M|M|C|M|M|M|C|M|M|M|M|
   *                     |11|10|9|8|7|1|6|5|4|0|3|2|1|0|
   *                     +--+--+-+-+-+-+-+-+-+-+-+-+-+-+
   *              Figure 3: Format of STUN Message Type Field
   * Here the bits in the message type field are shown as most significant
   * (M11) through least significant (M0).  M11 through M0 represent a 12-
   * bit encoding of the method.  C1 and C0 represent a 2-bit encoding of
   * the class.  A class of 0b00 is a request, a class of 0b01 is an
   * indication, a class of 0b10 is a success response, and a class of
   * 0b11 is an error response.
   */
  public int getMessageClass() {
    // Who in the holy hell thought this was a good idea for a protocol? The
    // `class` is a 2 bit value constructed from the lowest bit of the highest
    // order byte and the 5th lowest bit of the 2nd highester order byte.
    return (((int)data[0] & 0x01) << 1) | ((int)data[1] & 0x10) >> 4;
  }

  /**
   * Gets the message method from the header message type. See notes in
   * {@code #getMessageClass}.
   */
  public int getMessageMethod() {
    // Like its shitty cousin, the `method` is the 12 bit value (from lowest to
    // highest bit) constructed from the M0-M3 bits, M4-M6 bits, M7-M11 bits.
    return (((int)data[0] & 0x3e) << 6) | // M7-M11
        (((int)data[1] & 0xe0) >> 1) | // M4-M6
        (((int)data[1] & 0x0f)); // M0-M3
  }

  /**
   * @see https://tools.ietf.org/html/rfc5389#section-6
   *
   * The message length MUST contain the size, in bytes, of the message
   * not including the 20-byte STUN header.  Since all STUN attributes are
   * padded to a multiple of 4 bytes, the last 2 bits of this field are
   * always zero.
   */
  public int getMessageLength() {
    return Bytes.twoBytesToInt(data[2], data[3]);
  }

  public int getTotalMessageLength() {
    return getMessageLength() + MESSAGE_LEN_HEADER;
  }

  public boolean hasAttributes() {
    return getMessageLength() > 0;
  }

  /**
   * @see https://tools.ietf.org/html/rfc5389#section-6
   *
   * The magic cookie field MUST contain the fixed value 0x2112A442 in
   * network byte order.  In RFC 3489 [RFC3489], this field was part of
   * the transaction ID; placing the magic cookie in this location allows
   * a server to detect if the client will understand certain attributes
   * that were added in this revised specification.  In addition, it aids
   * in distinguishing STUN packets from packets of other protocols when
   * STUN is multiplexed with those other protocols on the same port.
   */
  public boolean hasMagicCookie() {
    return Bytes.fourBytesToInt(data[4], data[5], data[6], data[7]) ==
        MAGIC_COOKIE_FIXED_VALUE;
  }

  /**
   * Adding an explicit predicate for detecting legacy messages. See comment on
   * {@code hasMagicCookie}.
   */
  public boolean isRFC5389Message() {
    return hasMagicCookie();
  }

  /**
   * @see https://tools.ietf.org/html/rfc5389#section-6
   *
   * The transaction ID is a 96-bit identifier, used to uniquely identify
   * STUN transactions.  For request/response transactions, the
   * transaction ID is chosen by the STUN client for the request and
   * echoed by the server in the response.  For indications, it is chosen
   * by the agent sending the indication.  It primarily serves to
   * correlate requests with responses, though it also plays a small role
   * in helping to prevent certain types of attacks.  The server also uses
   * the transaction ID as a key to identify each transaction uniquely
   * across all clients.  As such, the transaction ID MUST be uniformly
   * and randomly chosen from the interval 0 .. 2**96-1, and SHOULD be
   * cryptographically random.
   */
  public byte[] getTransactionId() {
    byte[] id = new byte[MESSAGE_LEN_TRANSACTION_ID];
    System.arraycopy(data, 8, id, 0, MESSAGE_LEN_TRANSACTION_ID);
    return id;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof Message)) {
      return false;
    } else if (this == other) {
      return true;
    }
    return Arrays.equals(data, ((Message)other).data);
  }

  public boolean equalTransactionID(Message message) {
    byte[] idHeader = message.getTransactionId();
    if (idHeader.length != MESSAGE_LEN_TRANSACTION_ID)
      return false;
    byte[] tId = getTransactionId();
    if ((idHeader[0] == tId[0]) && (idHeader[1] == tId[1]) && (idHeader[2] == tId[2]) && (idHeader[3] == tId[3]) && (idHeader[4] == tId[4]) && (idHeader[5] == tId[5]) && (idHeader[6] == tId[6])
            && (idHeader[7] == tId[7]) && (idHeader[8] == tId[8]) && (idHeader[9] == tId[9]) && (idHeader[10] == tId[10]) && (idHeader[11] == tId[11])) {
      return true;
    } else {
      return false;
    }
  }

  /** Builds a success response for a request with the given attributes. */
  public Message buildSuccessResponse(byte... responseAttrs) {
    return buildResponse(true /* isSuccess */, responseAttrs);
  }

  /** Builds an error response for a request with the given attributes. */
  public Message buildErrorResponse(byte... responseAttrs) {
    return buildResponse(false /* isSuccess */, responseAttrs);
  }

  /**
   * Builds a response to a request message. If this object is not a request a
   * runtime exception will be thrown. The response will be filled in with this
   * message's method and transaction id. The success or error response class
   * will be determined by the {@code isSuccess} parameter. All attribute bytes
   * passed in {@param responseAttrs} will be added to the response.
   */
  private Message buildResponse(boolean isSuccess, byte... responseAttrs) {
    Preconditions.checkState(getMessageClass() == MESSAGE_CLASS_REQUEST);
    return builder()
        .setMessageClass(isSuccess ? MESSAGE_CLASS_RESPONSE : MESSAGE_CLASS_ERROR_RESPONSE)
        .setMessageMethod(getMessageMethod())
        .setTransactionId(getTransactionId())
        .setAttributeBytes(responseAttrs)
        .build();
  }

  public Message buildIndication(byte... responseAttrs) {
    Preconditions.checkState(getMessageClass() == MESSAGE_CLASS_INDICATION);
    return builder()
            .setMessageClass(MESSAGE_CLASS_INDICATION)
            .setMessageMethod(getMessageMethod())
            .setTransactionId(getTransactionId())
            .setAttributeBytes(responseAttrs)
            .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private int messageClass;
    private int messageMethod;
    private int length;
    private byte[] attributeBytes;
    private byte[] transactionIdBytes;

    private Builder() {
      messageClass = 0;
      messageMethod = 0;
      length = 0;
    }

    /** Sets the method and validates it is in the valid range 0 - 3 */
    public Builder setMessageClass(int messageClass) {
      Preconditions.checkArgument(0 <= messageClass && messageClass <= 3);
      this.messageClass = messageClass;
      return this;
    }

    /** Sets the method and validates it is in the valid range 0 - 0xfff */
    public Builder setMessageMethod(int messageMethod) {
      Preconditions.checkArgument(0 <= messageMethod && messageMethod <= 0xfff);
      this.messageMethod = messageMethod;
      return this;
    }

    /**
     * Sets the bytes to be held in the message attributes, and sets the message
     * length as computed from the length of the attribute bytes array. Since
     * all attributes are padded to a multiple of 4 bytes, the attributes array
     * length is validatd as such.
     */
    public Builder setAttributeBytes(byte[] attributeBytes) {
      int attributesLength = attributeBytes.length;
      Preconditions.checkArgument(attributesLength % 4 == 0);
      this.attributeBytes = new byte[attributesLength];
      System.arraycopy(attributeBytes, 0, this.attributeBytes, 0, attributesLength);

      this.length = attributesLength;

      return this;
    }

    /**
     * Sets the transaction id. Validates the the length of the byte array is
     * the RFC 5839 defined size for transaction id, 12 bytes.
     */
    public Builder setTransactionId(byte[] transactionIdBytes) {
      Preconditions.checkArgument(transactionIdBytes.length == MESSAGE_LEN_TRANSACTION_ID);
      this.transactionIdBytes = new byte[MESSAGE_LEN_TRANSACTION_ID];
      System.arraycopy(
          transactionIdBytes, 0, this.transactionIdBytes, 0, MESSAGE_LEN_TRANSACTION_ID);
      return this;
    }

    public Builder generateTransactionID() throws IllegalArgumentException {
      this.transactionIdBytes = new byte[MESSAGE_LEN_TRANSACTION_ID];
      System.arraycopy(Bytes.intToBytes((int) (Math.random() * 65536), 2), 0, transactionIdBytes, 0, 2);
      System.arraycopy(Bytes.intToBytes((int) (Math.random() * 65536), 2), 0, transactionIdBytes, 2, 2);
      System.arraycopy(Bytes.intToBytes((int) (Math.random() * 65536), 2), 0, transactionIdBytes, 4, 2);
      System.arraycopy(Bytes.intToBytes((int) (Math.random() * 65536), 2), 0, transactionIdBytes, 6, 2);
      System.arraycopy(Bytes.intToBytes((int) (Math.random() * 65536), 2), 0, transactionIdBytes, 8, 2);
      System.arraycopy(Bytes.intToBytes((int) (Math.random() * 65536), 2), 0, transactionIdBytes, 10, 2);
      return this;
    }

    /**
     * Builds a new Message and discards all data set in the builder.
     */
    public Message build() {
      Objects.requireNonNull(transactionIdBytes);

      byte[] messageBytes = new byte[MESSAGE_LEN_HEADER + length];

      byte[] messageTypeBytes = createMessageType();
      System.arraycopy(messageTypeBytes, 0, messageBytes, MESSAGE_POS_TYPE, MESSAGE_LEN_TYPE);

      byte[] lengthBytes = Bytes.intToBytes(length);
      System.arraycopy(lengthBytes, 2, messageBytes, MESSAGE_POS_LENGTH, MESSAGE_LEN_LENGTH);

      byte[] magicCookieBytes = Bytes.intToBytes(MAGIC_COOKIE_FIXED_VALUE);
      System.arraycopy(
          magicCookieBytes, 0, messageBytes, MESSAGE_POS_MAGIC_COOKIE, MESSAGE_LEN_MAGIC_COOKIE);

      System.arraycopy(
          transactionIdBytes, 0, messageBytes, MESSAGE_POS_TRANSACTION_ID,
          MESSAGE_LEN_TRANSACTION_ID);
      transactionIdBytes = null;

      if (attributeBytes != null && attributeBytes.length > 0) {
        System.arraycopy(
            attributeBytes, 0, messageBytes, MESSAGE_LEN_HEADER, attributeBytes.length);
        attributeBytes = null;
      }

      return new Message(messageBytes);
    }

    /**
     * This is the opposite process of {@code #getMessageClass} and
     * {@code #getMessageMethod}. Wish me luck.
     *
     * A message method is an int in the range 0 - 0xfff, so given the max
     * value int:
     *   00000000 00000000 00001111 11111111
     *                         abcd efghijkl
     *
     * And given a message class with the range 0-3, so the max value int:
     *   00000000 00000000 00000000 00000011
     *                                    AB
     *
     * The message type to construct is a 16 bit value as follows:
     *   00ab cdeA fghB ijkl
     */
    private byte[] createMessageType() {
      byte byteHigh = (byte)(
          ((messageMethod >>> 6) & 0x3e) | // `abcde` bits
          ((messageClass >>> 1) & 0x01)); // `A` bit

      byte byteLow = (byte)(
          ((messageMethod << 1) & 0xe0) | // `fgh` bits
          ((messageClass << 4) & 0x10) | // `B` bit
          (messageMethod & 0x0f)); // `ijkl` bits

      return new byte[] {byteHigh, byteLow};
    }

  }
}
