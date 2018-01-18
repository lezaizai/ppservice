package io.vos.stun.testing;

import io.vos.stun.message.Message;

import com.google.common.io.BaseEncoding;

public final class Messages {

  public static byte[] hexToBytes(String hexString) {
    return BaseEncoding.base16().lowerCase().decode(hexString);
  }

  public static Message createMessage(String hexString) {
    return new Message(hexToBytes(hexString));
  }

  /**
   * {@see https://tools.ietf.org/html/rfc5769#section-2.1}
   *
   * 2.1. Sample Request
   * This request uses the following parameters:
   * Software name:  "STUN test client" (without quotes)
   * Username:  "evtj:h6vY" (without quotes)
   * Password:  "VOkJxbRl1RmTxUk/WvJxBt" (without quotes)
   */
  public static final String SAMPLE_REQUEST_1 =
      "00010058" + // Request type and message length
      "2112a442" + // Magic cookie
      "b7e7a701" + // }
      "bc34d686" + // }  Transaction ID
      "fa87dfae" + // }
      "80220010" + // SOFTWARE attribute header
      "5354554e" + // }
      "20746573" + // }  "STUN test client"
      "7420636c" + // }
      "69656e74" + // }
      "00240004" + // PRIORITY attribute header
      "6e0001ff" + // ICE priority value
      "80290008" + // ICE-CONTROLLED attribute header
      "932ff9b1" + // }  Pseudo-random tie breaker...
      "51263b36" + // }   ...for ICE control
      "00060009" + // USERNAME attribute header
      "6576746a" + // }
      "3a683676" + // }  Username (9 bytes) and padding (3 bytes)
      "59202020" + // }
      "00080014" + // MESSAGE-INTEGRITY attribute header
      "9aeaa70c" + // }
      "bfd8cb56" + // }
      "781ef2b5" + // }  HMAC-SHA1 fingerprint
      "b2d3f249" + // }
      "c1b571a2" + // }
      "80280004" + // FINGERPRINT attribute header
      "e57a3bcf";  // CRC32 fingerprint

  /** No Attribute Message. */
  public static final String NO_ATTRIBUTE_MESSAGE =
      "00010058" + // Request type and message length
      "2112a442" + // Magic cookie
      "b7e7a701" + // }
      "bc34d686" + // }  Transaction ID
      "fa87dfae";  // }

  /** A binding request with not attributes . */
  public static final String EMPTY_BINDING_REQUEST =
      "00010000" + // Request type and message length
      "2112a442" + // Magic cookie
      "b7e7a701" + // }
      "bc34d686" + // }  Transaction ID
      "fa87dfae";  // }

  /** A binding response with not attributes . */
  public static final String EMPTY_BINDING_SUCCESS_RESPONSE =
      "01010000" + // Request type and message length
      "2112a442" + // Magic cookie
      "b7e7a701" + // }
      "bc34d686" + // }  Transaction ID
      "fa87dfae";  // }


  /** A message with multiple attributes of the same type. */
  public static final String MESSAGE_DUPLICATE_ATTRIBUTES =
      "0001007f" + // Request type and message length
      "2112a442" + // Magic cookie
      "b7e7a701" + // }
      "bc34d686" + // }  Transaction ID
      "fa87dfae" + // }
      "80220010" + // SOFTWARE attribute header
      "5354554e" + // }
      "20746573" + // }  "STUN test client"
      "7420636c" + // }
      "69656e74" + // }
      "80220010" + // SOFTWARE attribute header 2
      "4a617661" + // }
      "20535455" + // }  "JAVA test client"
      "4e20636c" + // }
      "69656e74" + // }
      "80220010" + // SOFTWARE attribute header 3
      "486f6d65" + // }
      "20737765" + // }
      "65742048" + // }
      "6f6d65";    // }
}
