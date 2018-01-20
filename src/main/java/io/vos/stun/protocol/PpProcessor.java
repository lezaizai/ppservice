package io.vos.stun.protocol;

import com.didlink.db.UserLocationDAO;
import com.didlink.models.UserLocation;
import io.vos.stun.attribute.Attribute;
import io.vos.stun.attribute.AttributesCollection;
import io.vos.stun.attribute.LocationAttribute;
import io.vos.stun.attribute.MappedAddressAttribute;
import io.vos.stun.util.Bytes;

import javax.xml.stream.Location;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.vos.stun.attribute.Attributes.ATTRIBUTE_LOCATION;
import static io.vos.stun.attribute.Attributes.ATTRIBUTE_XOR_MAPPED_ADDRESS;
import static io.vos.stun.message.Messages.*;

final class PpProcessor extends BaseMethodProcessor {
  private static final Logger LOGGER = Logger
          .getLogger(PpProcessor.class.getName());

    UserLocationDAO userLocationDAO;

  PpProcessor() {
    super(MESSAGE_METHOD_PP, MESSAGE_CLASS_REQUEST, MESSAGE_CLASS_INDICATION, MESSAGE_CLASS_RESPONSE);
    userLocationDAO = new UserLocationDAO();
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

    LocationAttribute locationAttribute = (LocationAttribute) attributes.getFirstAttributeOfType(ATTRIBUTE_LOCATION);

    if (locationAttribute == null) {
      LOGGER.log(Level.WARNING, "no location information.");
    } else {
      UserLocation userLocation = new UserLocation(locationAttribute.getUid(),
              locationAttribute.getLatitude(),
              locationAttribute.getLongitude(),
              locationAttribute.getLocatetime());
      try {
        userLocationDAO.saveLocation(userLocation);
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "user location information is not saved.", ex);

      }
    }

    return attributes.replyBuilder()
        .removeAllAttributesByType(ATTRIBUTE_XOR_MAPPED_ADDRESS)
        .addAttribute(mappedAddress)
        .build()
        .toByteArray();
  }

  @Override
  protected byte[] processIndicationInternal(RequestContext requestContext) {
    AttributesCollection attributes = requestContext.getAttributesCollection();

    Attribute mappedAddress = requestContext.getMessage().isRFC5389Message()
            ? getXorMappedAddress(requestContext)
            : getMappedAddress(requestContext);
    return AttributesCollection.EMPTY_COLLECTION.replyBuilder()
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
