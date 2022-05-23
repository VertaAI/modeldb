package ai.verta.modeldb.common.exceptions;

import com.google.rpc.Code;
import org.springframework.http.HttpStatus;

public class UnimplementedException extends ModelDBException {
  public UnimplementedException(String message) {
    super(message, Code.UNIMPLEMENTED, HttpStatus.NOT_IMPLEMENTED);
  }
}
