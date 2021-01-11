package ai.verta.modeldb.exceptions;

import com.google.rpc.Code;

public class UnimplementedException extends ModelDBException {
  public UnimplementedException(String message) {
    super(message, Code.UNIMPLEMENTED);
  }
}
