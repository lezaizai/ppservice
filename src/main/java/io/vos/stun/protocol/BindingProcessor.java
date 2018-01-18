package io.vos.stun.protocol;

import static io.vos.stun.attribute.Attributes.*;
import static io.vos.stun.message.Messages.*;

import io.vos.stun.attribute.Attribute;
import io.vos.stun.attribute.AttributesCollection;
import io.vos.stun.attribute.MappedAddressAttribute;
import io.vos.stun.util.Bytes;

import java.net.*;

final class BindingProcessor extends BaseMethodProcessor {

  BindingProcessor() {
    super(MESSAGE_METHOD_BINDING, MESSAGE_CLASS_REQUEST, MESSAGE_CLASS_RESPONSE);
  }

  /**
   * Creates a new binding response for either 3489 or 5389 binding requests.
   */
  @Override
  protected byte[] processRequestInternal(RequestContext requestContext) {
    AttributesCollection attributes = requestContext.getAttributesCollection();

    Attribute mappedAddress = requestContext.getMessage().isRFC5389Message()
        ? getXorMappedAddress(requestContext)
        : getMappedAddress(requestContext);
    return attributes.replyBuilder()
        .removeAllAttributesByType(ATTRIBUTE_XOR_MAPPED_ADDRESS)
        .addAttribute(mappedAddress)
        .build()
        .toByteArray();
  }

  @Override
  protected byte[] processResponseInternal(RequestContext requestContext) {
    AttributesCollection attributes = requestContext.getAttributesCollection();

    MappedAddressAttribute mappedAttri =
            (MappedAddressAttribute)attributes.getFirstAttributeOfType(ATTRIBUTE_XOR_MAPPED_ADDRESS);

    Attribute mappedAddress = requestContext.getMessage().isRFC5389Message()
            ? getXorMappedAddress(mappedAttri.getMappedAddress(),
              mappedAttri.getPort(),
              mappedAttri.isIPv4() ? MappedAddressAttribute.AF_IPV4 : MappedAddressAttribute.AF_IPV6,
              requestContext.getMessage().getTransactionId())
            : getMappedAddress(requestContext);

    return attributes.replyBuilder()
            .removeAllAttributesByType(ATTRIBUTE_XOR_MAPPED_ADDRESS)
            .addAttribute(mappedAddress)
            .build()
            .toByteArray();
  }

  @Override
  protected InetSocketAddress getResponseAddressInternal(RequestContext requestContext) {
    return requestContext.getReplyAddress();
  }

  private Attribute getMappedAddress(RequestContext requestContext) {
    InetSocketAddress replyAddress = requestContext.getReplyAddress();
    byte[] portBytes = Bytes.intToBytes(replyAddress.getPort());
    byte[] addressBytes = replyAddress.getAddress().getAddress();
    int addressFamily = addressBytes.length == 4
        ? MappedAddressAttribute.AF_IPV4
        : MappedAddressAttribute.AF_IPV6;
    return MappedAddressAttribute.
        createAttribute(addressFamily, portBytes, addressBytes, false /* isXor */);
  }

  private Attribute getXorMappedAddress(RequestContext requestContext) {
    InetSocketAddress replyAddress = requestContext.getReplyAddress();

    //System.out.println(String.format("getXorMappedAddress Address: %s %d", replyAddress.getAddress(), replyAddress.getPort()));

    byte addressFamily;
    byte[] addressBytes = replyAddress.getAddress().getAddress();
    if (replyAddress.getAddress() instanceof Inet4Address) {
      addressFamily = MappedAddressAttribute.AF_IPV4;
    } else if (replyAddress.getAddress() instanceof Inet6Address) {
      addressFamily = MappedAddressAttribute.AF_IPV6;
    } else {
      throw new AssertionError("Should either have an IPv4 or IPv6 address");
    }

    return getXorMappedAddress(addressBytes, replyAddress.getPort(), addressFamily, requestContext.getMessage().getTransactionId());
  }

  private Attribute getXorMappedAddress(byte[] addressBytes, int port, byte addressFamily, byte[] transactionID) {

    //System.out.println(String.format("getXorMappedAddress Address: %s %d %d %s", new Address(addressBytes).toString(), port, addressFamily, transactionID));

    byte[] magicCookieBytes = Bytes.intToBytes(MAGIC_COOKIE_FIXED_VALUE);
    byte[] portBytes = Bytes.intToBytes(port);
    byte[] xPortBytes = new byte[] {
      (byte)(portBytes[2] ^ magicCookieBytes[0]),
      (byte)(portBytes[3] ^ magicCookieBytes[1])
    };

    byte[] xAddressBytes = new byte[addressBytes.length];
    byte[] xorBytes;
    if (addressFamily == MappedAddressAttribute.AF_IPV4) {
      xorBytes = magicCookieBytes;
    } else if (addressFamily == MappedAddressAttribute.AF_IPV6) {
      xorBytes = com.google.common.primitives.Bytes.concat(
          magicCookieBytes, transactionID);
    } else {
      throw new AssertionError("Should either have an IPv4 or IPv6 address");
    }

    for (int i = 0; i < addressBytes.length; i++) {
      xAddressBytes[i] = (byte)(addressBytes[i] ^ xorBytes[i]);
    }

    //System.out.println(String.format("getXorMappedAddress Address: %s %d", new Address(xAddressBytes).toString(), Bytes.twoBytesToInt(xPortBytes[0],xPortBytes[1])));

    return MappedAddressAttribute
        .createAttribute(addressFamily, xPortBytes, xAddressBytes, true /* isXor */);
  }
}
