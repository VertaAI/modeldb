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
  String ENABLE_LIQUIBASE_MIGRATION_ENV_VAR = "LIQUIBASE_MIGRATION";
  String RUN_LIQUIBASE_SEPARATE = "RUN_LIQUIBASE_SEPARATE";
  // AWS Releated Constants
  String AWS_ROLE_ARN = "AWS_ROLE_ARN";
  String AWS_WEB_IDENTITY_TOKEN_FILE = "AWS_WEB_IDENTITY_TOKEN_FILE";

  enum UserIdentifier {
    VERTA_ID,
    EMAIL_ID,
    USER_NAME
  }
}
