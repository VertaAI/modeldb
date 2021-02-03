package ai.verta.modeldb.common;

public interface CommonConstants {

  // String Constants
  String STRING_COLON = ":";
  String BACKGROUND_UTILS_COUNT = "backgroundUtilsCount";
  String UNSIGNED_USER = "unsigned_user";
  Integer MIN_CONNECTION_SIZE_DEFAULT = 5;
  Integer MAX_CONNECTION_SIZE_DEFAULT = 20;
  Integer CONNECTION_TIMEOUT_DEFAULT = 300;
  String INTERNAL_ERROR = "Internal server error";
  // Set to true to export the liquibase schema as sql statements
  Boolean EXPORT_SCHEMA = false;
  String userDir = "user.dir";
  String DELETED = "deleted";

  enum UserIdentifier {
    VERTA_ID,
    EMAIL_ID,
    USER_NAME
  }
}
