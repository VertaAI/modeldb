package ai.verta.modeldb.common.exceptions;

import io.grpc.Status.Code;

public class ModelDBException extends RuntimeException {

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

  protected ModelDBException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    code = Code.INTERNAL;
  }
}
