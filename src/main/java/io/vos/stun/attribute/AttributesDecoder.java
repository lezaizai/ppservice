package io.vos.stun.attribute;

import io.vos.stun.message.Message;
import io.vos.stun.util.Bytes;

import com.google.common.base.Preconditions;

/**
 * @see https://tools.ietf.org/html/rfc5389#section-15
 *
 * After the STUN header are zero or more attributes.  Each attribute
 * MUST be TLV encoded, with a 16-bit type, 16-bit length, and value.
 * Each STUN attribute MUST end on a 32-bit boundary.  As mentioned
 * above, all fields in an attribute are transmitted most significant
 * bit first.
 *
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |         Type                  |            Length             |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Value (variable)                ....
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 *                  Figure 4: Format of STUN Attributes
 */
public final class AttributesDecoder {

  private final AttributeFactory attributeFactory;

  public AttributesDecoder(AttributeFactory attributeFactory) {
    this.attributeFactory = Preconditions.checkNotNull(attributeFactory);
  }

  /**
   * Returns an immutable list of parsed attributes.
   */
  public AttributesCollection decodeMessageAttributes(Message message) {
    if (!message.hasAttributes()) {
      return AttributesCollection.EMPTY_COLLECTION;
    }

    byte[] attributeData = message.getAttributeBytes();

    Preconditions.checkState(attributeData.length % 4 == 0);
    AttributesCollection.Builder attrCollectionBuilder = AttributesCollection.builder();

    int currentByte = 0;
    while (currentByte < attributeData.length - 1) {
      int type = Bytes.twoBytesToInt(attributeData[currentByte++], attributeData[currentByte++]);
      int length = Bytes.twoBytesToInt(attributeData[currentByte++], attributeData[currentByte++]);

      byte[] valueData;
      if (length > 0) {
        int paddedLength = getPaddedLength(length);
        valueData = new byte[paddedLength];
        // we can just copy to length, because the valueData array is already
        // initialized to 0 byte values
        System.arraycopy(attributeData, currentByte, valueData, 0, length);
        currentByte += paddedLength;
      } else {
        valueData = new byte[0];
      }

      attrCollectionBuilder
          .addAttribute(attributeFactory.createAttribute(type, length, valueData));
    }
    return attrCollectionBuilder.build();
  }

  private static int getPaddedLength(int length) {
    int remainder = length % 4;
    return remainder == 0 ? length : length + 4 - remainder;
  }
}
