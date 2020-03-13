package ai.verta.modeldb;

public interface ModelDBMessages {
  String USER_NOT_FOUND_ERROR_MSG = "Could not find owner details for {}";
  String EXP_RUN_RECORD_COUNT_MSG = "ExperimentRunPaginationDTO record count : {}";
  String PROJECT_RECORD_COUNT_MSG = "ProjectPaginationDTO record count : {}";
  String DATASET_RECORD_COUNT_MSG = "DatasetPaginationDTO record count : {}";
  String MAPPING_FOUND_MSG = "Collaborator mappings found in Collaborator entity";
  String DATASET_UPDATE_SUCCESSFULLY_MSG = "Dataset updated successfully";
  String DATA_VERSION_NOT_FOUND_ERROR_MSG = "DatasetVersion not found for given ID";
  String EXPERIMENT_NOT_FOUND_ERROR_MSG = "Experiment not found for given ID : ";
  String EXP_RUN_NOT_FOUND_ERROR_MSG = "ExperimentRun not found for given ID";
  String JOB_NOT_FOUND_ERROR_MSG = "Job not found for given ID";
  String GETTING_PROJECT_BY_ID_MSG_STR = "Project by Id getting successfully";
  String GET_DATASET_VERSION_MSG = "Getting dataset version.";
  String VALUE_ALREADY_PRESENT_IN_DATASET_MSG = "Updated value is already present in Dataset";
  String ACCESS_IS_DENIDE_DATASET_VERSION_ENTITIES_MSG =
      "Access is denied. User is unauthorized for given DatasetVersion entities : ";
  String ACCESS_IS_DENIDE_DATASET_ENTITITY_MSG =
      "Access is denied. User is unauthorized for given Dataset entity";
  String LOCATION_TYPE_NOT_MATCH_OF_PATH_DATASET_VERSION_INFO =
      "Location type of PathDatasetVersionInfo does not match with PathDatasetVersionInfo of parent datsetVersion";
  String INVALID_DATSET_TYPE = "Invalid or missing Dataset Type in CreateDatasetVersion request";
  String DATASET_ID_NOT_FOUND_IN_REQUEST =
      "Dataset id not found in the request : " + ModelDBAuthInterceptor.METHOD_NAME.get();
  String DATASET_NAME_NOT_FOUND_IN_REQUEST =
      "Dataset name not found in the request : " + ModelDBAuthInterceptor.METHOD_NAME.get();
  String DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST =
      "Dataset id not found in the request : " + ModelDBAuthInterceptor.METHOD_NAME.get();
  String DATASET_VERSION_TYPE_NOT_MATCH_WITH_DATSET_TYPE =
      "Dataset version type does not match containing dataset type";
  String ACCESSIBLE_DATASET_IN_SERVICE = "Accessible datasets in service : {}";
  String DATSET_ALREADY_EXISTS_IN_DATABASE = "Dataset already exists in database";
  String READY_STATUS = "Setting isReady to true, was {}";
  String HOST_PORT_INFO_STR = "Host : {} Port : {}";
  String AUTH_SERVICE_REQ_SENT_MSG = "AuthService Request sent";
  String AUTH_SERVICE_RES_RECEIVED_MSG = "AuthService response received";
  String ROLE_SERVICE_RES_RECEIVED_MSG = "RoleService response received";
  String ROLE_SERVICE_RES_RECEIVED_TRACE_MSG = ROLE_SERVICE_RES_RECEIVED_MSG + " : {}";
  String CALL_TO_ROLE_SERVICE_MSG = "Making a call to RoleService";
  String MODIFICATION_OF_ORG_ADMIN_COLLABORATOR_DENIED =
      "Can not modify collaboration settings of organization admin user.";
}
