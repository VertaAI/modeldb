package ai.verta.modeldb.exceptions;

import com.google.rpc.Code;

public class InvalidArgumentException extends ModelDBException {
  public InvalidArgumentException(String message) {
    super(message, Code.INVALID_ARGUMENT);
  }
}
