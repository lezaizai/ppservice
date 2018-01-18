package io.vos.stun.attribute;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;

/**
 * @see https://www.iana.org/assignments/stun-parameters/stun-parameters.txt
 */
public final class Attributes {

  private Attributes() {}

  /** RFC 5389 Comprehension-required range (0x0000-0x7FFF): */
  public static final int ATTRIBUTE_RESERVED = 0x0000;
  public static final int ATTRIBUTE_MAPPED_ADDRESS = 0x0001;
  public static final int ATTRIBUTE_RESPONSE_ADDRESS = 0x0002; // deprecated
  public static final int ATTRIBUTE_CHANGE_ADDRESS = 0x0003;   // deprecated
  public static final int ATTRIBUTE_SOURCE_ADDRESS = 0x0004;   // deprecated
  public static final int ATTRIBUTE_CHANGED_ADDRESS = 0x0005;  // deprecated
  public static final int ATTRIBUTE_USERNAME = 0x0006;
  public static final int ATTRIBUTE_PASSWORD = 0x0007;         // deprecated
  public static final int ATTRIBUTE_MESSAGE_INTEGRITY = 0x0008;
  public static final int ATTRIBUTE_ERROR_CODE = 0x0009;
  public static final int ATTRIBUTE_UNKNOWN_ATTRIBUTES = 0x000A;
  public static final int ATTRIBUTE_REFLECTED_FROM = 0x000B;   // deprecated
  public static final int ATTRIBUTE_REALM = 0x0014;
  public static final int ATTRIBUTE_NONCE = 0x0015;
  public static final int ATTRIBUTE_XOR_MAPPED_ADDRESS = 0x0020;

  public static final int ATTRIBUTE_SIMPLE = 0x1001;
  public static final int ATTRIBUTE_DATA = 0x1002;
  public static final int ATTRIBUTE_FILEINFO = 0x1002;

  /** RFC 5389 Comprehension-optional range (0x8000-0xFFFF) */
  public static final int ATTRIBUTE_SOFTWARE = 0x8022;
  public static final int ATTRIBUTE_ALTERNATE_SERVER = 0x8023;
  public static final int ATTRIBUTE_FINGERPRINT = 0x8028;

  /**
   * Returns the requested attribute type from the list of attributes or null if
   * not found.
   */
  @Nullable
  public static Attribute findAttributeByType(Iterable<Attribute> attributes, final int type) {
    Predicate<Attribute> compareTypePredicate = new Predicate<Attribute>() {
        @Override
        public boolean apply(Attribute attr) {
          return attr.getType() == type;
        }
      };
    return Iterables.find(attributes, compareTypePredicate, null);
  }

  /**
   * Returns whether the given attribute type is in the comprehension-required
   * range. Throws a runtime-exception if the value of type is outside of the
   * valid attribute type range.
   */
  public static boolean isComprehensionRequired(int type) {
    if (type < 0 || type > 0xffff) {
      String errorMsg = "Attribute type outside of valid range 0x0000-0xffff, got: %d";
      throw new IllegalArgumentException(String.format(errorMsg, type));
    }
    return type <= 0x7fff;
  }

  /**
   * Returns whether this implementation supports this given attribute.
   */
  public static boolean isUnknownAttribute(Attribute attr) {
    return attr instanceof UnsupportedAttribute;
  }
}
