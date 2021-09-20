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
  public static final String ARTIFACT_STORE_DISABLED_LOGS = "Artifact store is disabled";
  public static final String BUCKET_DOES_NOT_EXISTS = "Bucket does not exists";
}
