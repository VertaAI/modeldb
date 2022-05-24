package ai.verta.modeldb.common.exceptions;

import io.grpc.Status.Code;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;

public class ModelDBException extends RuntimeException {

  private static final Logger LOGGER = LogManager.getLogger(ModelDBException.class);
  private static final long serialVersionUID = 1L;

  private final Code code;

  public ModelDBException() {
    super();
    code = Code.INTERNAL;
  }

  public ModelDBException(String message) {
    super(message);
    code = Code.INTERNAL;
  }

  public ModelDBException(String message, Throwable cause) {
    super(message, cause);
    code = Code.INTERNAL;
  }

  public ModelDBException(Throwable cause) {
    super(cause);
    code = Code.INTERNAL;
  }

  public ModelDBException(String message, Code code) {
    super(message);
    this.code = code;
  }

  public ModelDBException(String message, com.google.rpc.Code code, Throwable cause) {
    super(message, cause);
    this.code = Code.valueOf(code.name());
  }

  public ModelDBException(String message, com.google.rpc.Code code) {
    super(message);
    this.code = Code.valueOf(code.name());
  }

  public Code getCode() {
    return code;
  }

  public int getCodeValue() {
    return getCode().value();
  }

  protected ModelDBException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    code = Code.INTERNAL;
  }

  public HttpStatus getHttpCode() {
    return httpStatusFromCode(this.code);
  }

  public static HttpStatus httpStatusFromCode(Code code) {
    switch (code) {
      case OK:
        return HttpStatus.OK;
      case CANCELLED:
        return HttpStatus.REQUEST_TIMEOUT;
      case UNKNOWN:
      case INTERNAL:
      case DATA_LOSS:
        return HttpStatus.INTERNAL_SERVER_ERROR;
      case INVALID_ARGUMENT:
      case FAILED_PRECONDITION:
      case OUT_OF_RANGE:
        // Note, this deliberately doesn't translate to the similarly named '412 Precondition
        // Failed' HTTP response status.
        return HttpStatus.BAD_REQUEST;
      case DEADLINE_EXCEEDED:
        return HttpStatus.GATEWAY_TIMEOUT;
      case NOT_FOUND:
        return HttpStatus.NOT_FOUND;
      case ALREADY_EXISTS:
      case ABORTED:
        return HttpStatus.CONFLICT;
      case PERMISSION_DENIED:
        return HttpStatus.FORBIDDEN;
      case UNAUTHENTICATED:
        return HttpStatus.UNAUTHORIZED;
      case RESOURCE_EXHAUSTED:
        return HttpStatus.TOO_MANY_REQUESTS;
      case UNIMPLEMENTED:
        return HttpStatus.NOT_IMPLEMENTED;
      case UNAVAILABLE:
        return HttpStatus.SERVICE_UNAVAILABLE;
      default:
        LOGGER.info("Unknown gRPC error code: {}", code);
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }
}
