package ai.verta.modeldb.common.exceptions;

import io.grpc.Status.Code;
import org.springframework.http.HttpStatus;

public class ModelDBException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final Code code;

  private final HttpStatus httpCode;

  public ModelDBException() {
    super();
    code = Code.INTERNAL;
    httpCode = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public ModelDBException(String message) {
    super(message);
    code = Code.INTERNAL;
    httpCode = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public ModelDBException(String message, Throwable cause) {
    super(message, cause);
    code = Code.INTERNAL;
    httpCode = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public ModelDBException(Throwable cause) {
    super(cause);
    code = Code.INTERNAL;
    httpCode = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public ModelDBException(String message, Code code, HttpStatus httpCode) {
    super(message);
    this.code = code;
    this.httpCode = httpCode;
  }

  public ModelDBException(String message, com.google.rpc.Code code, HttpStatus httpCode, Throwable cause) {
    super(message, cause);
    this.code = Code.valueOf(code.name());
    this.httpCode = httpCode;
  }

  public ModelDBException(String message, com.google.rpc.Code code, HttpStatus httpCode) {
    super(message);
    this.code = Code.valueOf(code.name());
    this.httpCode = httpCode;
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
    httpCode = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public HttpStatus getHttpCode() {
    return httpCode;
  }
}
