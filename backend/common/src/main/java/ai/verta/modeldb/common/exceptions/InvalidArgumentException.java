package ai.verta.modeldb.common.exceptions;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.rpc.Code;

public class InvalidArgumentException extends ModelDBException {
  public InvalidArgumentException(String message) {
    super(message, Code.INVALID_ARGUMENT);
  }
}
