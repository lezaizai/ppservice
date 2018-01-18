package io.vos.stun.protocol;

final class ProtocolException extends Exception {

  public enum ReasonCode  {
    FIRST_TWO_BITS_NOT_ZERO(ErrorCode.ERROR_400),
    INVALID_MESSAGE_LENGTH(ErrorCode.ERROR_400),
    UNKNOWN_ATTRIBUTE(ErrorCode.ERROR_420),
    UNSUPPORTED_METHOD(ErrorCode.ERROR_500),
    UNSUPPORTED_CLASS_FOR_METHOD(ErrorCode.ERROR_500);

    private ErrorCode errorCode;

    ReasonCode(ErrorCode errorCode) {
      this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
      return errorCode;
    }
  }

  private final ReasonCode code;

  ProtocolException(ReasonCode code) {
    this(code, "");
  }

  ProtocolException(ReasonCode code, String message) {
    super(message);
    this.code = code;
  }

  public ReasonCode getReasonCode() {
    return code;
  }
}
