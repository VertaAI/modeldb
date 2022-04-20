package ai.verta.modeldb.common;

public interface CommonConstants {

  // String Constants
  String STRING_COLON = ":";
  String UNSIGNED_USER = "unsigned_user";
  String INTERNAL_ERROR = "Internal server error";
  // Set to true to export the liquibase schema as sql statements
  boolean EXPORT_SCHEMA = false;
  String USER_DIR = "user.dir";
  String DELETED = "deleted";
  String EMPTY_STRING = "";
  String S3 = "S3";
  Integer TAG_LENGTH = 40;
  Integer THREAD_COUNT = 256;
  String LIQUIBASE_MIGRATION = "LIQUIBASE_MIGRATION";
  String RUN_LIQUIBASE_SEPARATE = "RUN_LIQUIBASE_SEPARATE";
  String BACKEND_PID = "verta-backend.pid";

  enum UserIdentifier {
    VERTA_ID,
    EMAIL_ID,
    USER_NAME
  }
}
