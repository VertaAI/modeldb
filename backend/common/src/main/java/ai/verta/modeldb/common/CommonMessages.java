package ai.verta.modeldb.common;

public interface CommonMessages {

  String HOST_PORT_INFO_STR = "Host : {} Port : {}";
  String AUTH_SERVICE_CHANNEL_CLOSE_ERROR = "AuthServiceChannel close() error : ";
  String CALL_TO_ROLE_SERVICE_MSG = "Making a call to RoleService";
  String AUTH_SERVICE_REQ_SENT_MSG = "AuthService Request sent";
  String ROLE_SERVICE_RES_RECEIVED_MSG = "RoleService response received";
  String ROLE_SERVICE_RES_RECEIVED_TRACE_MSG = ROLE_SERVICE_RES_RECEIVED_MSG + " : {}";
  String AUTH_SERVICE_RES_RECEIVED_MSG = "AuthService response received";
  String READY_STATUS = "Setting isReady to true, was {}";
}
