package ai.verta.modeldb;

public abstract class ModelDBMessages {
  private ModelDBMessages() {}

  public static final String USER_NOT_FOUND_ERROR_MSG = "Could not find owner details for {}";
  public static final String EXP_RUN_RECORD_COUNT_MSG =
      "ExperimentRunPaginationDTO record count : {}";
  public static final String PROJECT_RECORD_COUNT_MSG = "ProjectPaginationDTO record count : {}";
  public static final String DATASET_RECORD_COUNT_MSG = "DatasetPaginationDTO record count : {}";
  public static final String DATASET_UPDATE_SUCCESSFULLY_MSG = "Dataset updated successfully";
  public static final String DATA_VERSION_NOT_FOUND_ERROR_MSG =
      "DatasetVersion not found for given ID";
  public static final String EXPERIMENT_NOT_FOUND_ERROR_MSG =
      "Experiment not found for given ID : ";
  public static final String EXP_RUN_NOT_FOUND_ERROR_MSG = "ExperimentRun not found for given ID";
  public static final String GETTING_PROJECT_BY_ID_MSG_STR = "Got Project by Id successfully";
  public static final String DATASET_ID_NOT_FOUND_IN_REQUEST = "Dataset id not found";
  public static final String DATASET_NAME_NOT_FOUND_IN_REQUEST = "Dataset name not found";
  public static final String DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST = "Dataset id not found";
  public static final String ACCESSIBLE_DATASET_IN_SERVICE = "Accessible datasets in service : {}";
  public static final String PROJECT_NOT_FOUND_FOR_ID = "Project not found for given ID";
  public static final String ACCESS_IS_DENIED_EXPERIMENT_NOT_FOUND_FOR_GIVEN_ID =
      "Access is denied. Experiment not found for given id : ";
  public static final String PERMISSION_DENIED = "Permission denied";
  public static final String UNIMPLEMENTED = "Unimplemented";
  public static final String UNKNOWN_VALUE_TYPE_RECOGNIZED_ERROR =
      "Unknown 'Value' type recognized";
  public static final String ITEMS_NOT_SPECIFIED_ERROR = "Items not specified";
  public static final String ACTIVE_REQUEST_COUNT_TRACE = "Active Request count {}";
  public static final String PROJECT_ID_NOT_PRESENT_ERROR = "Project ID not present";
  public static final String PROJECT_NOT_FOUND_FOR_GIVEN_ID_ERROR =
      "Project not found for given ID: ";
  public static final String ERROR_WHILE_INSERTION_ENTRY_ON_MODEL_DB_DEPLOYMENT_INFO_ERROR =
      "Error while insertion entry on ModelDB deployment info : {}";
  public static final String ERROR_WHILE_GETTING_DB_CONNECTION_ERROR =
      "Error while getting DB connection : {}";
  public static final String SWITCH_CASE_ARTIFACTS_DEBUG = "switch case : Artifacts";
  public static final String SWITCH_CASE_DATASETS_DEBUG = "switch case : Datasets";
  public static final String SWITCH_CASE_ATTRIBUTES_DEBUG = "switch case : Attributes";
  public static final String SWITCH_CASE_HYPERPARAMETERS_DEBUG = "switch case : Hyperparameters";
  public static final String SWITCH_CASE_METRICS_DEBUG = "switch case : Metrics";
  public static final String SWITCH_CASE_OBSERVATION_DEBUG = "switch case : Observation";
  public static final String SWITCH_CASE_FEATURE_DEBUG = "switch case : Feature";
  public static final String SWITCH_CASE_TAGS_DEBUG = "switch case : tags";
  public static final String DATASET_VERSION_NOT_FOUND_ERROR = "DatasetVersion not found";
}
