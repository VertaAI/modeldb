package ai.verta.modeldb;

public interface ModelDBConstants {

  // Properties Keys
  String VERTA_MODELDB_CONFIG = "VERTA_MODELDB_CONFIG";
  String VERTA_MODELDB_TEST_CONFIG = "VERTA_MODELDB_TEST_CONFIG";
  String ARTIFACT_STORE_CONFIG = "artifactStoreConfig";
  String ARTIFACT_STORE_TYPE = "artifactStoreType";
  String CLOUD_BUCKET_NAME = "cloudBucketName";
  String NFS = "NFS";
  String NFS_ROOT_PATH = "nfsRootPath";
  String PATH = "path";
  String PORT = "port";
  String S3 = "S3";
  String STORE_TYPE_PATH = "store_type_path";
  String LIQUIBASE_MIGRATION = "LIQUIBASE_MIGRATION";
  String RUN_LIQUIBASE_SEPARATE = "RUN_LIQUIBASE_SEPARATE";
  String LIMIT_RUN_ARTIFACT_NUMBER = "LIMIT_RUN_ARTIFACT_NUMBER: ";
  String LIMIT_RUN_NUMBER = "LIMIT_RUN_NUMBER: ";
  String LIMIT_RUN_ARTIFACT_SIZE = "LIMIT_RUN_ARTIFACT_SIZE: ";

  // AWS Releated Constants
  String AWS_ROLE_ARN = "AWS_ROLE_ARN";
  String AWS_WEB_IDENTITY_TOKEN_FILE = "AWS_WEB_IDENTITY_TOKEN_FILE";

  // Threshold Constant
  Integer NAME_MAX_LENGTH = 40;
  Integer NAME_LENGTH = 256;
  String PATH_DELIMITER = "/";
  Integer TAG_LENGTH = 40;

  String EMPTY_STRING = "";

  // Column/protos field names
  String ARTIFACTS = "artifacts";
  String ATTRIBUTES = "attributes";
  String DATASETS = "datasets";
  String DATE_CREATED = "date_created";
  String DATE_UPDATED = "date_updated";
  String ENTITY_ID = "entity_id";
  String ENTITY_NAME = "entity_name";
  String EXPERIMENT_ID = "experiment_id";
  String FEATURES = "features";
  String HYPERPARAMETERS = "hyperparameters";
  String ID = "id";
  String KEY = "key";
  String METADATA = "metadata";
  String METRICS = "metrics";
  String NAME = "name";
  String OBSERVATIONS = "observations";
  String OWNER = "owner";
  String PARENT_ID = "parent_id";
  String PATH_DATSET_VERSION_INFO = "path_dataset_version_info";
  String PROJECT_ID = "project_id";
  String PROJECT_IDS = "project_ids";
  String QUERY_DATSET_VERSION_INFO = "query_dataset_version_info";
  String RAW_DATSET_VERSION_INFO = "raw_dataset_version_info";
  String SHORT_NAME = "short_name";
  String TAGS = "tags";
  String VALUE = "value";
  String WORKSPACE = "workspace";
  String WORKSPACE_ID = "workspace_id";
  String WORKSPACE_NAME = "workspace_name";
  String WORKSPACE_TYPE = "workspace_type";
  String PROJECTS = "projects";
  String EXPERIMENTS = "experiments";
  String EXPERIMENT_RUNS = "experimentruns";
  String DATASETS_VERSIONS = "datasetversions";
  String COMMENTS = "comments";
  String CODEVERSIONS = "codeversions";
  String GIT_SNAPSHOTS = "gitsnapshots";
  String KEY_VALUES = "keyvalues";
  String TAG_MAPPINGS = "tagmappings";
  String VERSIONED_INPUTS = "versioned_inputs";
  String HYPERPARAMETER_ELEMENT_MAPPINGS = "hyperparameter_element_mappings";
  String MODEL_API_JSON = "model_api.json";
  String REQUIREMENTS_TXT = "requirements.txt";
  String VER_SPEC_PATTERN = "~=|==|!=|<=|>=|<|>|===";
  String VER_NUM_PATTERN = "a|b|rc|post|pre";

  // Common verb constants
  String ORDER_ASC = "asc";
  String ORDER_DESC = "desc";
  String GET = "get";
  String PUT = "put";
  String POST = "post";

  // Common constants
  String ARTIFACT_MAPPING = "artifactMapping";
  String ATTRIBUTE_MAPPING = "attributeMapping";
  String CODE_ARCHIVE = "code_archive";
  String CODE_VERSION = "code_version";
  String DATASET_ID = "dataset_id";
  String DATASET_IDS = "dataset_ids";
  String DATA_LIST = "data_list";
  String DATE_TIME = "date_time";
  String DATASET_ID_STR = "datasetId";
  String DATASET_VERSION_ID_STR = "datasetVersionId";
  String EMAILID = "email_id";
  String EXPERIMENT_ID_STR = "experimentId";
  String EXPERIMENT_RUN_ID_STR = "experimentRunId";
  String FIELD_TYPE_STR = "fieldType";
  String FEILD_TYPE = "field_type";
  String GIT_SNAPSHOT = "git_snapshot";
  String KEY_VALUE_MAPPING = "keyValueMapping";
  String LINKED_ARTIFACT_ID = "linked_artifact_id";
  String OBSERVATION_MAPPING = "observationMapping";
  String PROJECT_ID_STR = "projectId";
  String TOTAL_COUNT = "total_count";
  String TIME_CREATED = "time_created";
  String TIME_UPDATED = "time_updated";
  String TIME_LOGGED = "time_logged";
  String VERSION = "version";
  String VERTA_ID = "verta_id";
  String USERNAME = "username";
  String DELETED = "deleted";
  String CREATED = "created";

  // Common error messages
  String ACCESS_DENIED_EXPERIMENT_RUN = "User does not have access to the ExperimentRun.";
  String NON_EQ_ID_PRED_ERROR_MESSAGE =
      "Only equality predicates supported on ids. Use EQ Operator.";
  String INTERNAL_MSG_USERS_NOT_FOUND = "MDB Users not found.";

  // Relational Query alias
  String ARTIFACT_ALIAS = "_art_";
  String ATTRIBUTE_ALIAS = "_attr_";
  String DATASET_ALIAS = "_dat_";
  String FEATURE_ALIAS = "ft_";
  String HYPERPARAMETER_ALIAS = "_hypr_";
  String METRICS_ALIAS = "_met_";
  String OBSERVATION_ALIAS = "_ob_";
  String TAGS_ALIAS = "tm_";
  String VERSIONED_ALIAS = "ver_";

  // Migration Constants
  String ENABLE = "enable";
  String SUB_ENTITIES_OWNERS_RBAC_MIGRATION = "SUB_ENTITIES_OWNERS_RBAC_MIGRATION";
  String ROLE_REPOSITORY_READ_WRITE = "REPOSITORY_READ_WRITE";
  String ROLE_REPOSITORY_READ_ONLY = "REPOSITORY_READ_ONLY";
  String SUB_ENTITIES_REPOSITORY_OWNERS_RBAC_MIGRATION =
      "SUB_ENTITIES_REPOSITORY_OWNERS_RBAC_MIGRATION";
  String DATASET_VERSIONING_MIGRATION = "DATASET_VERSIONING_MIGRATION";
  String DIFFERENT_REPOSITORY_OR_COMMIT_MESSAGE =
      "Can't add new versioning entry, because an existing one has different repository or commit";
  String REPOSITORY_ENTITY = "repositoryEntity";
  String POPULATE_VERSION_MIGRATION = "POPULATE_VERSION_MIGRATION";

  // Role name
  String ROLE_DATASET_CREATE = "DATASET_CREATE";
  String ROLE_DATASET_OWNER = "DATASET_OWNER";
  String ROLE_DATASET_READ_WRITE = "DATASET_READ_WRITE";
  String ROLE_DATASET_READ_ONLY = "DATASET_READ_ONLY";
  String ROLE_DATASET_PUBLIC_READ = "DATASET_PUBLIC_READ";
  String ROLE_PROJECT_CREATE = "PROJECT_CREATE";
  String ROLE_PROJECT_OWNER = "PROJECT_OWNER";
  String ROLE_PROJECT_READ_WRITE = "PROJECT_READ_WRITE";
  String ROLE_PROJECT_READ_ONLY = "PROJECT_READ_ONLY";
  String ROLE_PROJECT_PUBLIC_READ = "PROJECT_PUBLIC_READ";
  String ROLE_PROJECT_ADMIN = "PROJECT_ADMIN";
  String ROLE_EXPERIMENT_OWNER = "EXPERIMENT_OWNER";
  String ROLE_EXPERIMENT_RUN_OWNER = "EXPERIMENT_RUN_OWNER";
  String ROLE_DATASET_VERSION_OWNER = "DATASET_VERSION_OWNER";
  String ROLE_REPOSITORY_OWNER = "REPOSITORY_OWNER";
  String ROLE_REPOSITORY_ADMIN = "REPOSITORY_ADMIN";

  // Telemetry Constants
  String TELEMETRY_CONSUMER_URL =
      "https://app.verta.ai/api/v1/uac-proxy/telemetry/collectTelemetry";

  // Versioning constant
  String BLOB = "blob";
  String REPOSITORY_ID = "repository_id";
  String TAG = "tag";
  String ENTITY_HASH = "entity_hash";
  String ENTITY_TYPE = "entity_type";
  String LABEL = "label";
  String LABELS = "labels";
  String BRANCH = "branch";
  String BRANCH_NOT_FOUND = "Branch not found ";
  String COMMIT_NOT_FOUND = "Commit not found ";
  String INITIAL_COMMIT_MESSAGE = "Initial commit";
  String MASTER_BRANCH = "master";
  String COMMIT = "commit";
  String VERSIONING_LOCATION = "versioning_location";
  String VISIBILITY = "visibility";
  String REPOSITORY = "repository";
  String VERSIONING_REPOSITORY = "versioning_repository";
  String VERSIONING_REPO_COMMIT_BLOB = "versioning_repo_commit_blob";
  String DEFAULT_VERSIONING_BLOB_LOCATION = "version";
  String REPOSITORY_ACCESS_MODIFIER = "repositoryAccessModifier";
  String PROPERTY_NAME = "property_name";
  String FILENAME = "FileName";

  // Cron job constant
  String DELETE_ENTITIES = "delete_entities";
  String UPDATE_RUN_ENVIRONMENTS = "update_run_environments";
  String CLEAN_UP_ENTITIES = "clean_up_entities";

  // Audit log constants
  String CREATE = "CREATE";
  String UPDATE = "UPDATE";
  String DELETE = "DELETE";
  String SERVICE_NAME = "MDB";
  String PROJECT = "PROJECT";
  String EXPERIMENT = "EXPERIMENT";
  String EXPERIMENT_RUN = "EXPERIMENT_RUN";
  String DATASET = "DATASET";
  String DATASET_VERSION = "DATASET_VERSION";
  String COMMENT = "COMMENT";
}
