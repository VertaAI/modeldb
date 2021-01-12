package ai.verta.modeldb.common.exceptions;

import ai.verta.modeldb.exceptions.ModelDBException;
import com.google.rpc.Code;

public class UnavailableException extends ModelDBException {
  public UnavailableException(String message) {
    super(message, Code.UNAVAILABLE);
  }
}
