package ai.verta.modeldb.common;

public abstract class CommonMessages {

  public static final String ARTIFACT_STORE_DISABLED_LOGS = "Artifact store is disabled";
  public static final String BUCKET_DOES_NOT_EXIST = "Bucket does not exist";

  private CommonMessages() {}

  public static final String HOST_PORT_INFO_STR = "Host : {} Port : {}";
  public static final String AUTH_SERVICE_CHANNEL_CLOSE_ERROR =
      "AuthServiceChannel close() error : ";
  public static final String CALL_TO_ROLE_SERVICE_MSG = "Making a call to MDBRoleService";
  public static final String AUTH_SERVICE_REQ_SENT_MSG = "AuthService Request sent";
  public static final String ROLE_SERVICE_RES_RECEIVED_MSG = "MDBRoleService response received";
  public static final String ROLE_SERVICE_RES_RECEIVED_TRACE_MSG =
      ROLE_SERVICE_RES_RECEIVED_MSG + " : {}";
  public static final String AUTH_SERVICE_RES_RECEIVED_MSG = "AuthService response received";
  public static final String READY_STATUS = "Setting isReady to true, was {}";
  public static final String ENTITY_ID_IS_EMPTY_ERROR = "Entity id is empty";
  public static final String MISSING_REQUIRED = "required field is missing";
}
