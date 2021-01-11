package ai.verta.modeldb.exceptions;

import com.google.rpc.Code;

public class UnavailableException extends ModelDBException {
  public UnavailableException(String message) {
    super(message, Code.UNAVAILABLE);
  }
}
