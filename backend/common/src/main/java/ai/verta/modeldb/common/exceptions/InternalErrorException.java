package ai.verta.modeldb.common.exceptions;

import com.google.rpc.Code;

public class InternalErrorException extends ModelDBException {
  public InternalErrorException(String message) {
    super(message, Code.INTERNAL);
  }

  public InternalErrorException(String message, Exception cause) {
    super(message, Code.INTERNAL, cause);
  }
}
