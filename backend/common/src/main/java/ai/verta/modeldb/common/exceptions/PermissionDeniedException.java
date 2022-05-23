package ai.verta.modeldb.common.exceptions;

import com.google.rpc.Code;
import org.springframework.http.HttpStatus;

public class PermissionDeniedException extends ModelDBException {
  public PermissionDeniedException(String message) {
    super(message, Code.PERMISSION_DENIED, HttpStatus.FORBIDDEN);
  }
}
