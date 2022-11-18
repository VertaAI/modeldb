package ai.verta.modeldb.common.db.migration;

public class MigrationException extends Exception {
  public MigrationException(String message) {
    super(message);
  }

  public MigrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
