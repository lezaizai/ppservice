package io.vos.stun.attribute;

import static io.vos.stun.attribute.Attributes.*;

import io.vos.stun.util.Bytes;

import com.google.common.base.Preconditions;

import java.io.UnsupportedEncodingException;

/**
 * @see https://tools.ietf.org/html/rfc5389#section-15
 *
 * The ERROR-CODE attribute is used in error response messages.  It
 * contains a numeric error code value in the range of 300 to 699 plus a
 * textual reason phrase encoded in UTF-8 [RFC3629], and is consistent
 * in its code assignments and semantics with SIP [RFC3261] and HTTP
 * [RFC2616].  The reason phrase is meant for user consumption, and can
 * be anything appropriate for the error code.  Recommended reason
 * phrases for the defined error codes are included in the IANA registry
 * for error codes.  The reason phrase MUST be a UTF-8 [RFC3629] encoded
 * sequence of less than 128 characters (which can be as long as 763
 * bytes).
 *
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |           Reserved, should be 0         |Class|     Number    |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |      Reason Phrase (variable)                                ..
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 *                    Figure 7: ERROR-CODE Attribute
 */
public final class ErrorCodeAttribute extends BaseAttribute {

  private static final int MIN_ERROR_CODE = 300;
  private static final int MAX_ERROR_CODE = 699;

  ErrorCodeAttribute(int type, int length, byte[] valueData) {
    super(type, length, valueData);
  }

  private static byte[] PADDING = new byte[] {
    0x00, 0x00
  };

  public static ErrorCodeAttribute createAttribute(int errorCode, String reasonPhrase) {
    Preconditions.checkArgument(errorCode >= MIN_ERROR_CODE && errorCode <= MAX_ERROR_CODE);

    int errorClass = errorCode / 100;
    int errorNumber = errorCode % 100;

    byte[] errorClassBytes = new byte[] {
      Bytes.intToBytes(errorClass)[3]
    };
    byte[] errorNumberBytes = new byte[] {
      Bytes.intToBytes(errorNumber)[3]
    };
    byte[] reasonPhraseBytes;
    try {
      reasonPhraseBytes = reasonPhrase.getBytes("UTF-8");
    } catch(UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }

    byte[] valueData = com.google.common.primitives.Bytes.concat(
        PADDING, errorClassBytes, errorNumberBytes,
        reasonPhraseBytes);
    return new ErrorCodeAttribute(
        ATTRIBUTE_ERROR_CODE, valueData.length, Bytes.padTo4ByteBoundary(valueData));
  }
}
