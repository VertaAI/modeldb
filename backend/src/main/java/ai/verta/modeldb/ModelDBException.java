package ai.verta.modeldb;

import io.grpc.Status.Code;

public class ModelDBException extends Exception {

  private static final long serialVersionUID = 1L;

  private Code code = Code.INTERNAL;

  public ModelDBException() {
    super();
  }

  public ModelDBException(String message) {
    super(message);
  }

  public ModelDBException(String message, Throwable cause) {
    super(message, cause);
  }

  public ModelDBException(Throwable cause) {
    super(cause);
  }

  public ModelDBException(String message, Code code) {
    super(message);
    this.code = code;
  }

  public Code getCode() {
    return code;
  }

  protected ModelDBException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
