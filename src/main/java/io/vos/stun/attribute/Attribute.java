package io.vos.stun.attribute;

/** @see https://tools.ietf.org/html/rfc5389#section-15 */
public interface Attribute {

  /**
   * @see https://tools.ietf.org/html/rfc5389#section-15
   * @see https://www.iana.org/assignments/stun-parameters/stun-parameters.txt
   *
   * The STUN attribute type.
   */
  int getType();

  /**
   * @see https://tools.ietf.org/html/rfc5389#section-15
   *
   * Returns the length of the value as specified in the attribute bytes, note
   * that all attributes are padded to a 32 bit boundary, so this is not the
   * actual length of the byte array returned from {@code #getValue}.
   */
  int getLength();

  /**
   * Gets the total length in bytes of the attribute, as it appears in the
   * message, including the 2 byte type, 2 byte length, and value data.
   */
  int getTotalLength();

  /**
   * Returns the raw byte value portion of the data. This is variable length.
   */
  byte[] getValue();

  /**
   * @see https://tools.ietf.org/html/rfc5389#section-15
   *
   * To allow future revisions of this specification to add new attributes
   * if needed, the attribute space is divided into two ranges.
   * Attributes with type values between 0x0000 and 0x7FFF are
   * comprehension-required attributes, which means that the STUN agent
   * cannot successfully process the message unless it understands the
   * attribute.  Attributes with type values between 0x8000 and 0xFFFF are
   * comprehension-optional attributes, which means that those attributes
   * can be ignored by the STUN agent if it does not understand them.
   */
  boolean isComprehensionRequired();

  /**
   * Gets the byte array representation of the attribute.
   */
  byte[] toByteArray();
}
