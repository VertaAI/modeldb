package ai.verta.modeldb.common.exceptions;

import com.google.rpc.Code;
import org.springframework.http.HttpStatus;

public class InvalidArgumentException extends ModelDBException {
  public InvalidArgumentException(String message) {
    super(message, Code.INVALID_ARGUMENT, HttpStatus.BAD_REQUEST);
  }
}
