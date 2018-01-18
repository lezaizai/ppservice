package io.vos.stun.testing;

import io.vos.stun.attribute.Attribute;
import io.vos.stun.message.Message;
import io.vos.stun.protocol.BaseMethodProcessor;
import io.vos.stun.protocol.RequestContext;

public class FakeMethodProcessor extends BaseMethodProcessor {

  private Message processedRequest;

  public FakeMethodProcessor(int method, int... supportedClasses) {
    super(method, supportedClasses);
  }

  public Message getProcessedRequest() {
    return processedRequest;
  }

  @Override
  protected byte[] processRequestInternal(RequestContext requestContext) {
    processedRequest = requestContext.getMessage();
    return processedRequest.getBytes();
  }
}
