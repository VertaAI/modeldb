package ai.verta.modeldb;

public abstract class ModelDBConstants {
  private ModelDBConstants() {}

  // Properties Keys
  public static final String VERTA_MODELDB_CONFIG = "VERTA_MODELDB_CONFIG";
  public static final String VERTA_MODELDB_TEST_CONFIG = "VERTA_MODELDB_TEST_CONFIG";
  public static final String ARTIFACT_STORE_CONFIG = "MDBArtifactStoreConfig";
  public static final String ARTIFACT_STORE_TYPE = "artifactStoreType";
  public static final String CLOUD_BUCKET_NAME = "cloudBucketName";
  public static final String NFS = "NFS";
  public static final String NFS_ROOT_PATH = "nfsRootPath";
  public static final String PATH = "path";
  public static final String STORE_TYPE_PATH = "store_type_path";

  public static final Integer NAME_MAX_LENGTH = 40;
  public static final Integer NAME_LENGTH = 256;
  public static final String PATH_DELIMITER = "/";

  // Column/protos field names
  public static final String ARTIFACTS = "artifacts";
  public static final String ATTRIBUTES = "attributes";
  public static final String DATASETS = "datasets";
  public static final String DATE_CREATED = "date_created";
  public static final String DATE_UPDATED = "date_updated";
  public static final String ENTITY_ID = "entity_id";
  public static final String ENTITY_NAME = "entity_name";
  public static final String FEATURES = "features";
  public static final String HYPERPARAMETERS = "hyperparameters";
  public static final String ID = "id";
  public static final String KEY = "key";
  public static final String METADATA = "metadata";
  public static final String METRICS = "metrics";
  public static final String NAME = "name";
  public static final String OBSERVATIONS = "observations";
  public static final String OWNER = "owner";
  public static final String PATH_DATSET_VERSION_INFO = "path_dataset_version_info";
  public static final String PROJECT_IDS = "project_ids";
  public static final String QUERY_DATSET_VERSION_INFO = "query_dataset_version_info";
  public static final String RAW_DATSET_VERSION_INFO = "raw_dataset_version_info";
  public static final String TAGS = "tags";
  public static final String VALUE = "value";
  public static final String WORKSPACE = "workspace";
  public static final String WORKSPACE_ID = "workspace_id";
  public static final String WORKSPACE_NAME = "workspace_name";
  public static final String WORKSPACE_TYPE = "workspace_type";
  public static final String PROJECTS = "projects";
  public static final String EXPERIMENTS = "experiments";
  public static final String EXPERIMENT_TABLE_NAME = "experiment";
  public static final String EXPERIMENT_RUNS = "experimentruns";
  public static final String DATASETS_VERSIONS = "datasetversions";
  public static final String COMMENTS = "comments";
  public static final String CODEVERSIONS = "codeversions";
  public static final String GIT_SNAPSHOTS = "gitsnapshots";
  public static final String KEY_VALUES = "keyvalues";
  public static final String TAG_MAPPINGS = "tagmappings";
  public static final String VERSIONED_INPUTS = "versioned_inputs";
  public static final String MODEL_API_JSON = "model_api.json";
  public static final String REQUIREMENTS_TXT = "requirements.txt";
  public static final String VER_SPEC_PATTERN = "~=|==|!=|<=|>=|<|>|===";
  public static final String VER_NUM_PATTERN = "a|b|rc|post|pre";

  // Common verb constants
  public static final String POST = "post";

  // Common constants
  public static final String ARTIFACT_MAPPING = "artifactMapping";
  public static final String ATTRIBUTE_MAPPING = "attributeMapping";
  public static final String CODE_ARCHIVE = "code_archive";
  public static final String CODE_VERSION = "code_version";
  public static final String DATE_TIME = "date_time";
  public static final String FEILD_TYPE = "field_type";
  public static final String GIT_SNAPSHOT = "git_snapshot";
  public static final String KEY_VALUE_MAPPING = "keyValueMapping";
  public static final String LINKED_ARTIFACT_ID = "linked_artifact_id";
  public static final String OBSERVATION_MAPPING = "observationMapping";
  public static final String TIME_CREATED = "time_created";
  public static final String TIME_UPDATED = "time_updated";
  public static final String TIME_LOGGED = "time_logged";
  public static final String VERSION = "version";
  public static final String DELETED = "deleted";
  public static final String CREATED = "created";

  // Common error messages
  public static final String NON_EQ_ID_PRED_ERROR_MESSAGE =
      "Only equality predicates supported on ids. Use EQ Operator.";
  public static final String INTERNAL_MSG_USERS_NOT_FOUND = "MDB Users not found.";

  // Relational Query alias
  public static final String ARTIFACT_ALIAS = "_art_";
  public static final String ATTRIBUTE_ALIAS = "_attr_";
  public static final String DATASET_ALIAS = "_dat_";
  public static final String FEATURE_ALIAS = "ft_";
  public static final String HYPERPARAMETER_ALIAS = "_hypr_";
  public static final String METRICS_ALIAS = "_met_";
  public static final String OBSERVATION_ALIAS = "_ob_";
  public static final String TAGS_ALIAS = "tm_";
  public static final String VERSIONED_ALIAS = "ver_";

  // Migration Constants
  public static final String SUB_ENTITIES_OWNERS_RBAC_MIGRATION =
      "SUB_ENTITIES_OWNERS_RBAC_MIGRATION";
  public static final String DIFFERENT_REPOSITORY_OR_COMMIT_MESSAGE =
      "Can't add new versioning entry, because an existing one has different repository or commit";
  public static final String REPOSITORY_ENTITY = "repositoryEntity";
  public static final String POPULATE_VERSION_MIGRATION = "POPULATE_VERSION_MIGRATION";

  // Telemetry Constants
  public static final String TELEMETRY_CONSUMER_URL =
      "https://app.verta.ai/api/v1/uac-proxy/telemetry/collectTelemetry";

  // Versioning constant
  public static final String BLOB = "blob";
  public static final String REPOSITORY_ID = "repository_id";
  public static final String ENTITY_HASH = "entity_hash";
  public static final String ENTITY_TYPE = "entity_type";
  public static final String LABEL = "label";
  public static final String BRANCH_NOT_FOUND = "Branch not found ";
  public static final String COMMIT_NOT_FOUND = "Commit not found ";
  public static final String INITIAL_COMMIT_MESSAGE = "Initial commit";
  public static final String MASTER_BRANCH = "master";
  public static final String COMMIT = "commit";
  public static final String VISIBILITY = "visibility";
  public static final String REPOSITORY = "repository";
  public static final String VERSIONING_REPOSITORY = "versioning_repository";
  public static final String VERSIONING_REPO_COMMIT_BLOB = "versioning_repo_commit_blob";
  public static final String DEFAULT_VERSIONING_BLOB_LOCATION = "version";
  public static final String REPOSITORY_ACCESS_MODIFIER = "repositoryAccessModifier";
  public static final String PROPERTY_NAME = "property_name";

  // Cron job constant
  public static final String DELETE_ENTITIES = "delete_entities";
  public static final String UPDATE_RUN_ENVIRONMENTS = "update_run_environments";
  public static final String CLEAN_UP_ENTITIES = "clean_up_entities";

  // Audit log constants
  public static final String CREATE = "CREATE";
  public static final String UPDATE = "UPDATE";
  public static final String DELETE = "DELETE";
  public static final String SERVICE_NAME = "MDB";
  public static final String PROJECT = "PROJECT";
  public static final String EXPERIMENT = "EXPERIMENT";
  public static final String EXPERIMENT_RUN = "EXPERIMENT_RUN";
  public static final String DATASET = "DATASET";
  public static final String DATASET_VERSION = "DATASET_VERSION";
}
