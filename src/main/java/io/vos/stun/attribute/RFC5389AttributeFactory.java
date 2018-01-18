package io.vos.stun.attribute;

import static io.vos.stun.attribute.Attributes.*;

public final class RFC5389AttributeFactory implements AttributeFactory {

  public RFC5389AttributeFactory() {}

  @Override
  public Attribute createAttribute(int type, int length, byte[] valueData) {
    switch (type) {
      case ATTRIBUTE_MAPPED_ADDRESS:
      case ATTRIBUTE_XOR_MAPPED_ADDRESS:
        return new MappedAddressAttribute(type, length, valueData);
      case ATTRIBUTE_ERROR_CODE:
        return new ErrorCodeAttribute(type, length, valueData);
      case ATTRIBUTE_UNKNOWN_ATTRIBUTES:
        //        return new UnknownAttributesAttribute(type, length, valueData);
      default:
        return new UnsupportedAttribute(type, length, valueData);
    }
  }
}
