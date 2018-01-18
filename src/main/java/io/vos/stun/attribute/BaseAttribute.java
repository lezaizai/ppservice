package io.vos.stun.attribute;

import io.vos.stun.util.Bytes;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Objects;

public class BaseAttribute implements Attribute {

  private final int type;
  private final int length;
  private final byte[] valueData;

  public BaseAttribute(int type, int length, byte[] valueData) {
    Preconditions.checkArgument(type >= 0 && type <= 0xFFFF);
    Preconditions.checkNotNull(valueData);
    Preconditions.checkArgument(
        valueData.length % 4 == 0,
        String.format(
            "Attribute value data not on a mod-4 byte boundary, %d bytes", valueData.length));
    validateInternal(type, length, valueData);

    this.type = type;
    this.length = length;
    this.valueData = new byte[valueData.length];
    System.arraycopy(valueData, 0, this.valueData, 0, length);
  }

  /**
   * Constructor validation hook, meant to be override by subclasses. The
   * subclass MUST NOT modify the valueData array as it is being passed raw.
   */
  protected void validateInternal(int type, int length, byte[] valueData) {}

  @Override
  public final int getType() {
    return type;
  }

  @Override
  public final int getLength() {
    return length;
  }

  @Override
  public final int getTotalLength() {
    // 4 equals 2 bytes for the type and 2 bytes for the length.
    return 4 + valueData.length;
  }

  @Override
  public final byte[] getValue() {
    byte[] valueData = new byte[length];
    System.arraycopy(this.valueData, 0, valueData, 0, length);
    return valueData;
  }

  @Override
  public final boolean isComprehensionRequired() {
    return Attributes.isComprehensionRequired(type);
  }

  @Override
  public final byte[] toByteArray() {
    byte[] typeBytes = Bytes.intToBytes(type);
    byte[] lengthBytes = Bytes.intToBytes(length);
    byte[] headerData = new byte[] {
      typeBytes[2], typeBytes[3],
      lengthBytes[2], lengthBytes[3]
    };
    return com.google.common.primitives.Bytes.concat(headerData, valueData);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(type, length, Arrays.hashCode(valueData));
  }

  @Override
  public final boolean equals(Object other) {
    if (other == null || !(other instanceof Attribute)) {
      return false;
    } else if (this == other) {
      return true;
    }

    if (other instanceof BaseAttribute) {
      // great, faster comparison, important for storage in the
      // {@link AttributeCollection}.
      BaseAttribute baseAttrOther = (BaseAttribute) other;
      return type == baseAttrOther.type
          && length == baseAttrOther.length
          && Arrays.equals(valueData, baseAttrOther.valueData);
    }

    // Oh well, slower comparison.
    Attribute attrOther = (Attribute) other;
    return type == attrOther.getType()
        && length == attrOther.getLength()
        && Arrays.equals(valueData, attrOther.getValue());
  }
}
