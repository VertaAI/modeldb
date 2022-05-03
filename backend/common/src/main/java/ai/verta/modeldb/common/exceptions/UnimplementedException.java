package ai.verta.modeldb.common.exceptions;

import com.google.rpc.Code;

public class UnimplementedException extends ModelDBException {
  public UnimplementedException(String message) {
    super(message, Code.UNIMPLEMENTED);
  }
}
