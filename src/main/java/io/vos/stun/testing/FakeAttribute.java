package io.vos.stun.testing;

import io.vos.stun.attribute.BaseAttribute;

public class FakeAttribute extends BaseAttribute {

  public FakeAttribute(int type, int length, byte[] valueData) {
    super(type, length, valueData);
  }
}
