package io.vos.stun.protocol;

import io.vos.stun.attribute.AttributesCollection;
import io.vos.stun.attribute.FileInfoAttribute;
import io.vos.stun.message.Message;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.vos.stun.attribute.Attributes.ATTRIBUTE_FILEINFO;
import static io.vos.stun.message.Messages.*;

final class TransferFileProcessor extends BaseMethodProcessor {
  private final int MAX_PACKET_SIZE = 1024;

  private final int SEND_WINDOW = 5;

  private final String PATH = "files/";

  TransferFileProcessor() {
    super(MESSAGE_METHOD_TRANSFER_FILE, MESSAGE_CLASS_REQUEST, MESSAGE_CLASS_RESPONSE, MESSAGE_CLASS_INDICATION);
  }

  @Override
  protected byte[] processRequestInternal(RequestContext requestContext) {
    AttributesCollection attributes = requestContext.getAttributesCollection();
    FileInfoAttribute fileInfoAttri =
            (FileInfoAttribute)attributes.getFirstAttributeOfType(ATTRIBUTE_FILEINFO);

    System.out.println(fileInfoAttri.getPath() + fileInfoAttri.getSize());
    return null;
  }

  @Override
  protected byte[] processIndicationInternal(RequestContext requestContext) {
    return null;
  }

  @Override
  protected byte[] processResponseInternal(RequestContext requestContext) {
    return null;
  }

}
