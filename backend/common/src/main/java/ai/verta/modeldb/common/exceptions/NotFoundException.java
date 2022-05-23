package ai.verta.modeldb.common.exceptions;

import com.google.rpc.Code;
import org.springframework.http.HttpStatus;

public class NotFoundException extends ModelDBException {
  public NotFoundException(String message) {
    super(message, Code.NOT_FOUND, HttpStatus.NOT_FOUND);
  }
}
