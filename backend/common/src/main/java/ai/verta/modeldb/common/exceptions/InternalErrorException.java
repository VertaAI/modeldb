package ai.verta.modeldb.common.exceptions;

import ai.verta.modeldb.exceptions.ModelDBException;
import com.google.rpc.Code;

public class InternalErrorException extends ModelDBException {
  public InternalErrorException(String message) {
    super(message, Code.INTERNAL);
  }
}
