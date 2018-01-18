package io.vos.stun.protocol;

import static io.vos.stun.message.Messages.*;

import io.vos.stun.attribute.AttributesCollection;
import io.vos.stun.attribute.AttributesDecoder;
import io.vos.stun.attribute.ErrorCodeAttribute;
import io.vos.stun.attribute.RFC5389AttributeFactory;
import io.vos.stun.message.Message;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * A generic STUN agent that follows the message processing rules described in
 * RFC 5389.
 *
 * {@see https://tools.ietf.org/html/rfc5389#section-7.3}
 */
public class Agent implements MessageHandler {

  private final Map<Integer, MethodProcessor> registeredMethodProcessors;
  private final AttributesDecoder attributeDecoder;

  public Agent(Iterable<? extends MethodProcessor> methodProcessors) {
    registeredMethodProcessors = Maps.<Integer, MethodProcessor>newHashMap();
    for (MethodProcessor p : methodProcessors) {
      Preconditions.checkNotNull(p);
      int method = p.getMethod();
      Preconditions.checkState(!registeredMethodProcessors.containsKey(method));
      registeredMethodProcessors.put(method, p);
    }

    attributeDecoder = new AttributesDecoder(new RFC5389AttributeFactory());
  }

  public final int totalBytesInMessage(byte[] tlvCheck) {
    return Message.lengthCheck(tlvCheck);
  }

  @Override
  public final void onMessage(
          byte[] messageData, InetSocketAddress remoteAddress, ResponseHandler responseHandler) {
    Message message = null;
    try {
      message = new Message(Preconditions.checkNotNull(messageData));
      validateMessage(message);

      AttributesCollection attributes = attributeDecoder.decodeMessageAttributes(message);

      // TODO: this is where method authentication would go, since this is just
      // meant to be used as a basic server now I'll skip it. In the future to
      // support auth methods w/ database lookups, etc this class should be
      // refactored to have an onAuthenticatedMessage method, where the processing
      // code below would go.

      MethodProcessor proc =
              Preconditions.checkNotNull(registeredMethodProcessors.get(message.getMessageMethod()));

      switch (message.getMessageClass()) {
        case MESSAGE_CLASS_REQUEST:
          RequestContext requestContext =  new RequestContext(message, attributes, remoteAddress);

          byte[] responseAttributeBytes =  proc.processRequest(requestContext);
          Message response = message.buildSuccessResponse(responseAttributeBytes);
          InetSocketAddress responseAddress = proc.getResponseAddress(requestContext);

          responseHandler.onQuest(response.getBytes(), responseAddress.getAddress(), responseAddress.getPort());
          break;
        case MESSAGE_CLASS_INDICATION:
          RequestContext indicationContext =  new RequestContext(message, attributes, remoteAddress);

          byte[] indicationAttributeBytes =  proc.processIndication(indicationContext);
          Message indication = message.buildIndication(indicationAttributeBytes);
          InetSocketAddress indicationAddress = proc.getResponseAddress(indicationContext);

          responseHandler.onIndication(indication.getBytes(), indicationAddress.getAddress(), indicationAddress.getPort());
          break;
        case MESSAGE_CLASS_RESPONSE:
          byte[] responseMapAddrBytes =
                  proc.processResponse(new RequestContext(message, attributes, remoteAddress));
          responseHandler.onResponse(responseMapAddrBytes, remoteAddress.getAddress(), remoteAddress.getPort());
          break;
        case MESSAGE_CLASS_ERROR_RESPONSE:
          proc.processError(message, attributes);
          break;
        default:
          throw new AssertionError("Handling invalid message class, this should have been validated");
      }
    } catch (ProtocolException e) {
      if (message != null && message.getMessageClass() == MESSAGE_CLASS_REQUEST) {
        responseHandler.onResponse(getErrorResponse(message, e.getReasonCode().getErrorCode()), remoteAddress.getAddress(), remoteAddress.getPort());
      }
      e.printStackTrace();
    }
  }

  private byte[] getErrorResponse(Message message, ErrorCode errorCode) {
    ErrorCodeAttribute errorCodeAttr =
        ErrorCodeAttribute.createAttribute(errorCode.getCode(), errorCode.getStatus());

    Message errorMessage = message.buildErrorResponse(Bytes.concat(
        message.getAttributeBytes(), errorCodeAttr.toByteArray()));
    return errorMessage.getBytes();
  }

  /**
   * Validates the message according to RFC 5389. Throws a ProtocolException if the message is
   * invalid.
   */
  private void validateMessage(Message message) throws ProtocolException {
    if (!message.hasNonZeroHeaderBits()) {
      byte[] headerBytes = message.getHeaderBytes();
      String errorMsg = String.format(
          "Expected two leading 0 bits, bit 0: %d, byte 1: %d", headerBytes[0], headerBytes[1]);
      throw new ProtocolException(ProtocolException.ReasonCode.FIRST_TWO_BITS_NOT_ZERO, errorMsg);
    }

    int msgLength = message.getMessageLength();
    int actualMessageLength = message.getAttributeBytes().length;
    if (msgLength != actualMessageLength) {
      String errorMsg = String.format(
          "message length from header was %d but was actually %d", msgLength, actualMessageLength);
      throw new ProtocolException(ProtocolException.ReasonCode.INVALID_MESSAGE_LENGTH, errorMsg);
    }

    int msgMethod = message.getMessageMethod();
    if (!registeredMethodProcessors.containsKey(msgMethod)) {
      String errorMsg =
          String.format("unsupported message method %d", msgMethod);
      throw new ProtocolException(ProtocolException.ReasonCode.UNSUPPORTED_METHOD, errorMsg);
    }

    int msgClass = message.getMessageClass();
    if (!registeredMethodProcessors.get(msgMethod).isClassSupported(msgClass)) {
      String errorMsg =
          String.format("unsupported message class %d for method %d", msgClass, msgMethod);
      throw new ProtocolException(
          ProtocolException.ReasonCode.UNSUPPORTED_CLASS_FOR_METHOD, errorMsg);
    }

  }

  public static Agent createBasicServer() {
    return new Agent(Lists.newArrayList(new BindingProcessor(), new NegociatingProcessor(), new TransferFileProcessor()));
  }
}
