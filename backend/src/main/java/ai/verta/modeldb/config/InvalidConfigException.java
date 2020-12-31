package ai.verta.modeldb.config;

import ai.verta.modeldb.exceptions.ModelDBException;
import com.google.rpc.Code;

public class InvalidConfigException extends ModelDBException {
  public InvalidConfigException(String location, String message) {
    super(String.format("Validation failure at %s: %s", location, message), Code.INTERNAL);
  }
}
