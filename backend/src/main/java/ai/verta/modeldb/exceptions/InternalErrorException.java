package ai.verta.modeldb.exceptions;

import com.google.rpc.Code;

public class InternalErrorException extends ModelDBException {
  public InternalErrorException(String message) {
    super(message, Code.INTERNAL);
  }
}
