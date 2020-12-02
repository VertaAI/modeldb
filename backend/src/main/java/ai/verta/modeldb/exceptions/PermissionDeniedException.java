package ai.verta.modeldb.exceptions;

import com.google.rpc.Code;

public class PermissionDeniedException extends ModelDBException {
  public PermissionDeniedException(String message) {
    super(message, Code.PERMISSION_DENIED);
  }
}
