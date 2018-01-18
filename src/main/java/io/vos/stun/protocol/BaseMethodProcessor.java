package io.vos.stun.protocol;

import static io.vos.stun.message.Messages.*;

import io.vos.stun.attribute.Attribute;
import io.vos.stun.attribute.Attributes;
import io.vos.stun.message.Message;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The base implementation of a MethodProcessor. Performs the appropriate
 * validation on attributes for each message class according to RFC
 * 5389. Subclasses are expected to override the internal methods for processing
 * various messages classes.
 */
public class BaseMethodProcessor implements MethodProcessor {

  private final int method;
  private final int[] supportedClasses;

  /**
   * Creates a new BaseMethodProcessor for the given {@code method} and handles
   * the {@code supportedClasses}. Throws a runtime exception if
   * {@code supportedClasses} is empty.
   */
  public BaseMethodProcessor(int method, int... supportedClasses) {
    this.method = method;

    Preconditions.checkNotNull(supportedClasses);
    Preconditions.checkArgument(supportedClasses.length > 0);
    this.supportedClasses = new int[supportedClasses.length];
    System.arraycopy(supportedClasses, 0, this.supportedClasses, 0, supportedClasses.length);
  }

  @Override
  public final int getMethod() {
    return method;
  }

  @Override
  public final boolean isClassSupported(int methodClass) {
    return Arrays.binarySearch(supportedClasses, methodClass) >= 0;
  }

  /**
   * Called after attributes are validated in {@code #processRequest}. Override
   * in a subclass.
   */
  protected byte[] processRequestInternal(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }

  /**
   * Called after attributes are validated in {@code #processIndication}. Override
   * in a subclass.
   */
  protected byte[] processIndicationInternal(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }

  /**
   * Called after attributes are validated in {@code #processResponse}. Override
   * in a subclass.
   */
  protected byte[] processResponseInternal(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }

  protected InetSocketAddress getResponseAddressInternal(RequestContext requestContext) {
    throw new UnsupportedOperationException();
  }

  /**
   * Called after attributes are validated in {@code #processError}. Override
   * in a subclass.
   */
  protected void processErrorInternal(Message message, Iterable<Attribute> attributes) {}

  @Override
  public byte[] processRequest(RequestContext requestContext) throws ProtocolException {
    if (!isClassSupported(MESSAGE_CLASS_REQUEST)) {
      throw new UnsupportedOperationException();
    }

    // From RFC 5389, section 7.3.1
    // If the request contains one or more unknown comprehension-required
    // attributes, the server replies with an error response with an error
    // code of 420 (Unknown Attribute), and includes an UNKNOWN-ATTRIBUTES
    // attribute in the response that lists the unknown comprehension-
    // required attributes.
    List<Integer> unknownAttributeTypes = findUnknownAttributeTypes(requestContext.getAttributes());
    if (unknownAttributeTypes.size() > 0) {
      String errorMsg = String.format(
          "Unknown required attribute types %s", Joiner.on(",").join(unknownAttributeTypes));
      throw new ProtocolException(ProtocolException.ReasonCode.UNKNOWN_ATTRIBUTE, errorMsg);
    }

    return processRequestInternal(requestContext);
  }

  @Override
  public byte[] processIndication(RequestContext requestContext) throws ProtocolException{
    if (!isClassSupported(MESSAGE_CLASS_INDICATION)) {
      throw new UnsupportedOperationException();
    }

    List<Integer> unknownAttributeTypes = findUnknownAttributeTypes(requestContext.getAttributes());
    if (unknownAttributeTypes.size() > 0) {
      String errorMsg = String.format(
              "Unknown required attribute types %s", Joiner.on(",").join(unknownAttributeTypes));
      throw new ProtocolException(ProtocolException.ReasonCode.UNKNOWN_ATTRIBUTE, errorMsg);
    }

    return processIndicationInternal(requestContext);
  }

  @Override
  public byte[] processResponse(RequestContext requestContext) throws ProtocolException {
    if (!isClassSupported(MESSAGE_CLASS_RESPONSE)) {
      throw new UnsupportedOperationException();
    }

    // From RFC 5389, section 7.3.3
    // If the success response contains unknown comprehension-required
    // attributes, the response is discarded and the transaction is
    // considered to have failed.
    List<Integer> unknownAttributeTypes = findUnknownAttributeTypes(requestContext.getAttributes());
    if (unknownAttributeTypes.size() > 0) {
      String errorMsg = String.format(
              "Unknown required attribute types %s", Joiner.on(",").join(unknownAttributeTypes));
      throw new ProtocolException(ProtocolException.ReasonCode.UNKNOWN_ATTRIBUTE, errorMsg);
    }

    return processResponseInternal(requestContext);
  }

  @Override
  public void processError(Message message, Iterable<Attribute> attributes) {
    if (!isClassSupported(MESSAGE_CLASS_ERROR_RESPONSE)) {
      throw new UnsupportedOperationException();
    } else if (hasUnknownAttributes(attributes)) {
      // From RFC 5389, section 7.3.4
      // If the error response contains unknown comprehension-required
      // attributes, or if the error response does not contain an ERROR-CODE
      // attribute, then the transaction is simply considered to have failed.
      return;
    }

    processErrorInternal(message, attributes);
  }

  @Override
  public InetSocketAddress getResponseAddress(RequestContext requestContext) throws ProtocolException {
    return getResponseAddressInternal(requestContext);
  }

  /** Returns true if any attribute is unsupported and required. */
  private boolean hasUnknownAttributes(Iterable<Attribute> attributes) {
    for (Attribute a : attributes) {
      if (Attributes.isUnknownAttribute(a) && a.isComprehensionRequired()) {
        return true;
      }
    }
    return false;
  }

  /** Returns a List of all attributes that are unsupported and required. */
  private List<Integer> findUnknownAttributeTypes(Iterable<Attribute> attributes) {
    List<Integer> unknownAttributeTypes = new ArrayList<>();
    for (Attribute a : attributes) {
      if (Attributes.isUnknownAttribute(a) && a.isComprehensionRequired()) {
        unknownAttributeTypes.add(a.getType());
      }
    }
    return unknownAttributeTypes;
  }
}
