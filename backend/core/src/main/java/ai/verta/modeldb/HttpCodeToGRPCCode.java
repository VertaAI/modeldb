package ai.verta.modeldb;

import com.google.rpc.Code;

public class HttpCodeToGRPCCode {
  public static Code convertHTTPCodeToGRPCCode(int httpCode) {
    if (httpCode == 200) {
      return Code.OK;
    } else if (httpCode >= 400 && httpCode <= 499) {
      switch (httpCode) {
        case 499: // CANCELLED : 499 Client Closed Request
          return Code.CANCELLED;
        case 404: // NOT_FOUND: 404 Not Found
          return Code.NOT_FOUND;
        case 403: // PERMISSION_DENIED: 403 Forbidden
          return Code.PERMISSION_DENIED;
        case 409: // ALREADY_EXISTS: 409 Conflict
          return Code.ALREADY_EXISTS;
        case 429: // RESOURCE_EXHAUSTED: 429 Too Many Requests
          return Code.RESOURCE_EXHAUSTED;
        case 401: // UNAUTHENTICATED: 401 Unauthorized
          return Code.UNAUTHENTICATED;
        case 400: // INVALID_ARGUMENT: 400 Bad Request
        default:
          return Code.INVALID_ARGUMENT;
      }
    } else if (httpCode >= 500 && httpCode <= 599) {
      switch (httpCode) {
        case 504: // DEADLINE_EXCEEDED: 504 Gateway Timeout
          return Code.DEADLINE_EXCEEDED;
        case 501: // UNIMPLEMENTED: 501 Not Implemented
          return Code.UNIMPLEMENTED;
        case 503: // UNAVAILABLE: 503 Service Unavailable
          return Code.UNAVAILABLE;
        default:
          return Code.INTERNAL;
      }
    } else {
      return Code.INTERNAL;
    }
  }
}
