package io.vos.stun.protocol;

import io.vos.stun.attribute.Attribute;
import io.vos.stun.message.Message;

import java.net.InetSocketAddress;

/**
 * Implementations handle any/all message classes for a single method.
 */
public interface MethodProcessor {

  /** Returns the int value of the method, as defined by IANA. */
  int getMethod();

  /**
   * Returns true if the MethodProcessor handles the message class. This should
   * be used by clients before calling a processXxx method, to determine if the
   * processor can process the message class. Otherwise a runtime exception will
   * be thrown.
   */
  boolean isClassSupported(int messageClass);

  /**
   * Processes the message in the RequestContext. Throws a runtime exception for
   * any message with a method other than that returned by {@code #getMethod},
   * or if a request is not supported for the method, as returned by {@code
   * isClassSupported}. Any exception will generate an error response. The
   * return attribute bytes to use in the success response.
   */
  byte[] processRequest(RequestContext requestContext) throws ProtocolException;

  /**
   * Processes the given message. Throws a runtime exception for any message
   * with a method other than that returned by {@code #getMethod}, or if a
   * indication is not supported for the method, as returned by
   * {@code isClassSupported}.
   */
  byte[] processIndication(RequestContext requestContext) throws ProtocolException;

  /**
   * Processes the given message. Throws a runtime exception for any message
   * with a method other than that returned by {@code #getMethod}, or if a
   * response is not supported for the method, as returned by
   * {@code isClassSupported}.
   */
  byte[] processResponse(RequestContext requestContext) throws ProtocolException;

  /**
   * Processes the given message. Throws a runtime exception for any message
   * with a method other than that returned by {@code #getMethod}, or if a
   * error is not supported for the method, as returned by
   * {@code isClassSupported}.
   */
  void processError(Message message, Iterable<Attribute> attributes);

  InetSocketAddress getResponseAddress(RequestContext requestContext) throws ProtocolException;

}
