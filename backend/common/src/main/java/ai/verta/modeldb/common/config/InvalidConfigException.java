package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.rpc.Code;
import org.springframework.http.HttpStatus;

public class InvalidConfigException extends ModelDBException {
  public InvalidConfigException(String location, String message) {
    super(
        String.format("Validation failure at %s: %s", location, message),
        Code.INTERNAL,
        HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
