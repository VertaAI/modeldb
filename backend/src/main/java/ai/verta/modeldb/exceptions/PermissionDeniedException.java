package ai.verta.modeldb.exceptions;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.rpc.Code;

public class PermissionDeniedException extends ModelDBException {

  public PermissionDeniedException(String message) {
    super(message, Code.PERMISSION_DENIED);
  }
}
