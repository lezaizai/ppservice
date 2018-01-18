package io.vos.stun.protocol;

import io.vos.stun.attribute.Attribute;
import io.vos.stun.attribute.AttributesCollection;
import io.vos.stun.message.Message;

import java.net.InetSocketAddress;
import java.util.Objects;

public class RequestContext {

  private final Message message;
  private final Iterable<Attribute> attributes;
  private final InetSocketAddress replyAddress;

  // TODO: refactor this to just take an AttributesCollection and remove
  // #getAttributesCollection, I'm just too lazy to do it now.
  RequestContext(Message message, Iterable<Attribute> attributes, InetSocketAddress replyAddress) {
    this.message = Objects.requireNonNull(message);
    this.attributes = Objects.requireNonNull(attributes);
    this.replyAddress = Objects.requireNonNull(replyAddress);
  }

  public Message getMessage() {
    return message;
  }

  public Iterable<Attribute> getAttributes() {
    return attributes;
  }

  public AttributesCollection getAttributesCollection() {
    return (attributes instanceof AttributesCollection)
        ? (AttributesCollection)attributes
        : AttributesCollection.builder().addAllAttributes(attributes).build();
  }

  public InetSocketAddress getReplyAddress() {
    return replyAddress;
  }
}
