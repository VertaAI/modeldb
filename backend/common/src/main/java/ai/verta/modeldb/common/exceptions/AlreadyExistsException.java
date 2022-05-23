package ai.verta.modeldb.common.exceptions;

import com.google.rpc.Code;
import org.springframework.http.HttpStatus;

public class AlreadyExistsException extends ModelDBException {
  public AlreadyExistsException(String message) {
    super(message, Code.ALREADY_EXISTS, HttpStatus.CONFLICT);
  }
}
