package io.vos.stun.attribute;

public interface AttributeFactory {

  /**
   * Creates the Attribute instance indicated by type.
   */
  Attribute createAttribute(int type, int length, byte[] valueData);

}
