package io.vos.stun.attribute;

class UnsupportedAttribute extends BaseAttribute {

  UnsupportedAttribute(int type, int length, byte[] valueData) {
    super(type, length, valueData);
  }
}
