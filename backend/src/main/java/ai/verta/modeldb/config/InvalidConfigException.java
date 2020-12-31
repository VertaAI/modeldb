package ai.verta.modeldb.config;

public class InvalidConfigException extends Exception {
  public InvalidConfigException(String location, String message) {
    super(String.format("Validation failure at %s: %s", location, message));
  }
}
