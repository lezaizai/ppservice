package io.vos.stun.protocol;

/** STUN Error Codes */
enum ErrorCode {
                                                    // 0-299  Reserved
  ERROR_300(300, "Try Alternate"),                  // RFC5389
                                                    // 301-399 Unassigned
  ERROR_400(400, "Bad Request"),                    // RFC5389
  ERROR_401(401, "Unauthorized"),                   // RFC5389
                                                    // 402   Unassigned
  ERROR_403(403, "Forbidden"),                      // RFC5766
                                                    // 404-419 Unassigned
  ERROR_420(420, "Unknown Attribute"),              // RFC5389
                                                    // 421-436 Unassigned
  ERROR_437(437, "Allocation Mismatch"),            // RFC5766
  ERROR_438(438, "Stale Nonce"),                    // RFC5389
                                                    // 439   Unassigned
  ERROR_440(440, "Address Family not Supported"),   // RFC6156
  ERROR_441(441, "Wrong Credentials"),              // RFC5766
  ERROR_442(442, "Unsupported Transport Protocol"), // RFC5766
  ERROR_443(443, "Peer Address Family Mismatch"),   // RFC6156
                                                    // 444-445 Unassigned
  ERROR_446(446, "Connection Already Exists"),      // RFC6062
  ERROR_447(447, "Connection Timeout or Failure"),  // RFC6062
                                                    // 448-485 Unassigned
  ERROR_486(486, "Allocation Quota Reached"),       // RFC5766
  ERROR_487(487, "Role Conflict"),                  // RFC5245
                                                    // 488-499 Unassigned
  ERROR_500(500, "Server Error"),                   // RFC5389
                                                    // 501-507 Unassigned
  ERROR_508(508, "Insufficient Capacity");          // RFC5766
                                                    // 509-699 Unassigned

  private final int code;
  private final String status;

  ErrorCode(int code, String status) {
    this.code = code;
    this.status = status;
  }

  public int getCode() {
    return code;
  }

  public String getStatus() {
    return status;
  }
}
