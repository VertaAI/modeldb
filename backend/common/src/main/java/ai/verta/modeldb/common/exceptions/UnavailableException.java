package ai.verta.modeldb.common.exceptions;

import com.google.rpc.Code;
import org.springframework.http.HttpStatus;

public class UnavailableException extends ModelDBException {
  public UnavailableException(String message) {
    super(message, Code.UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
  }
}
