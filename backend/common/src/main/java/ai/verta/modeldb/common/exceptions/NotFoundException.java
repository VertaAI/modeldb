package ai.verta.modeldb.common.exceptions;

import com.google.rpc.Code;

public class NotFoundException extends ModelDBException {
  public NotFoundException(String message) {
    super(message, Code.NOT_FOUND);
  }
}
