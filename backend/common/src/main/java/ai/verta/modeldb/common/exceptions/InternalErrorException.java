package ai.verta.modeldb.common.exceptions;

import com.google.rpc.Code;
import org.springframework.http.HttpStatus;

public class InternalErrorException extends ModelDBException {
  public InternalErrorException(String message) {
    super(message, Code.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public InternalErrorException(String message, Exception cause) {
    super(message, Code.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR, cause);
  }
}
