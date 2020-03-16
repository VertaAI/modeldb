package ai.verta.modeldb;

public class ModelDBException extends Exception {

  private static final long serialVersionUID = 1L;

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

  protected ModelDBException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
