package ai.verta.modeldb.common.exceptions;

import com.google.rpc.Code;

public class AlreadyExistsException extends ModelDBException {
  public AlreadyExistsException(String message) {
    super(message, Code.ALREADY_EXISTS);
  }
}
