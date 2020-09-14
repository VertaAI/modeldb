package ai.verta.modeldb;

public interface ModelDBConstants {

  // Properties Keys
  String VERTA_MODELDB_CONFIG = "VERTA_MODELDB_CONFIG";
  String ARTIFACT_ENDPOINT = "artifactEndpoint";
  String ARTIFACT_STORE_CONFIG = "artifactStoreConfig";
  String ARTIFACT_STORE_TYPE = "artifactStoreType";
  String AUTH_SERVICE = "authService";
  String CLOUD_ACCESS_KEY = "cloudAccessKey";
  String CLOUD_SECRET_KEY = "cloudSecretKey";
  String CLOUD_BUCKET_NAME = "cloudBucketName";
  String MINIO_ENDPOINT = "minioEndpoint";
  String AWS_REGION = "aws_region";
  String DATABASE = "database";
  String DB_TYPE = "DBType";
  String DISABLED_MDB_COLLABORATOR = "disabled-mdb-collaborator";
  String FEATURE_FLAG = "feature-flag";
  String GET_ARTIFACT_ENDPOINT = "getArtifact";
  String GRPC_SERVER = "grpcServer";
  String HOST = "host";
  String HTTPS_STR = "https";
  String NFS = "NFS";
  String PICK_ARTIFACT_STORE_HOST_FROM_CONFIG = "pickArtifactStoreHostFromConfig";
  String PICK_NFS_HOST_FROM_CONFIG = "pickNFSHostFromConfig";
  String NFS_ROOT_PATH = "nfsRootPath";
  String ARTIFACT_STORE_SERVER_HOST = "artifactStoreServerHost";
  String ARTIFACT_STORE_URL_PROTOCOL = "artifactStoreUrlProtocol";
  String NFS_SERVER_HOST = "nfsServerHost";
  String NFS_URL_PROTOCOL = "nfsUrlProtocol";
  String PATH = "path";
  String PORT = "port";
  String RELATIONAL = "relational";
  String S3 = "S3";
  String SHUTDOWN_TIMEOUT = "shutdownTimeout";
  String SPRING_SERVER = "springServer";
  String STORE_TYPE_PATH = "store_type_path";
  String STARTER_PROJECT = "starterProject";
  String STARTER_PROJECT_ID = "starterProjectId";
  String STORE_ARTIFACT_ENDPOINT = "storeArtifact";
  String userDir = "user.dir";
  String MDB_SERVICE_USER = "mdb_service_user";
  String POPULATE_CONNECTIONS_BASED_ON_PRIVILEGES = "populateConnectionsBasedOnPrivileges";

  // AWS Releated Constants
  String AWS_ROLE_ARN = "AWS_ROLE_ARN";
  String DEFAULT_AWS_REGION = "us-east-1";
  String AWS_WEB_IDENTITY_TOKEN_FILE = "AWS_WEB_IDENTITY_TOKEN_FILE";
  String AWS_WEB_IDENTITY_TOKEN = "AWS_WEB_IDENTITY_TOKEN";

  // feature-flags
  String DISABLED_AUTHZ = "disabled-authz";
  String STORE_CLIENT_CREATION_TIMESTAMP = "store-client-creation-timestamp";
  String PUBLIC_SHARING_ENABLED = "public_sharing_enabled";
  String DISABLED_ARTIFACT_STORE = "disabled-artifact-store";

  // Threshold Constant
  Long DEFAULT_SHUTDOWN_TIMEOUT = 30L; // timeout in second
  Integer NAME_MAX_LENGTH = 40;
  Integer NAME_LENGTH = 256;
  String PATH_DELIMITER = "/";
  Integer TAG_LENGTH = 40;
  int INITIAL_CRON_DELAY = 300; // 300second = 5min : timeout in second
  String INITIAL_DELAY = "initial_delay";

  // String Constants
  String STRING_COLON = ":";
  String EMPTY_STRING = "";

  // Column/protos field names
  String ARTIFACTS = "artifacts";
  String ATTRIBUTES = "attributes";
  String DATASET_COLLABORATORS = "dataset_collaborators";
  String DATASETS = "datasets";
  String REPOSITORIES = "repositories";
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
  String PROJECT_COLLABORATORS = "project_collaborators";
  String PROJECT_ID = "project_id";
  String PROJECT_IDS = "project_ids";
  String PROJECT_VISIBILITY = "project_visibility";
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

  // Common verb constants
  String ORDER_ASC = "asc";
  String ORDER_DESC = "desc";
  String ADD = "add";
  String UPDATE = "update";
  String GET = "get";
  String DELETE = "delete";
  String PUT = "put";

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
  String DATASET_VISIBILITY = "dataset_visibility";
  String EMAILID = "email_id";
  String DATASET_VERSION_VISIBILITY = "dataset_version_visibility";
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
  String UNSIGNED_USER = "unsigned_user";
  String VERSION = "version";
  String VERTA_ID_STR = "vertaId";
  String VERTA_ID = "verta_id";
  String EMAIL = "email";
  String USERNAME = "username";
  String GRPC_HEALTH_CHECK_METHOD_NAME = "grpc.health.v1.Health/Check";
  String DELETED = "deleted";
  String DEV_KEY = "devKey";
  String REQUEST_TIMEOUT = "requestTimeout";

  // Set to true to export the liquibase schema as sql statements
  Boolean EXPORT_SCHEMA = false;

  // Common error messages
  String ACCESS_DENIED_EXPERIMENT_RUN = "User does not have access to the ExperimentRun.";
  String AUTH_SERVICE_CHANNEL_CLOSE_ERROR = "AuthServiceChannel close() error : ";
  String INTERNAL_ERROR = "Internal server error";
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
  String MIGRATION = "migration";
  String ENABLE = "enable";
  String SUB_ENTITIES_OWNERS_RBAC_MIGRATION = "SUB_ENTITIES_OWNERS_RBAC_MIGRATION";
  String ROLE_REPOSITORY_READ_WRITE = "REPOSITORY_READ_WRITE";
  String ROLE_REPOSITORY_READ_ONLY = "REPOSITORY_READ_ONLY";
  String SUB_ENTITIES_REPOSITORY_OWNERS_RBAC_MIGRATION =
      "SUB_ENTITIES_REPOSITORY_OWNERS_RBAC_MIGRATION";
  String DATASET_VERSIONING_MIGRATION = "DATASET_VERSIONING_MIGRATION";
  String POSTGRES_DB_DIALECT = "org.hibernate.dialect.PostgreSQLDialect";
  String DIFFERENT_REPOSITORY_OR_COMMIT_MESSAGE =
      "Can't add new versioning entry, because an existing one has different repository or commit";
  String REPOSITORY_ENTITY = "repositoryEntity";
  String POPULATE_VERSION_MIGRATION = "POPULATE_VERSION_MIGRATION";

  enum UserIdentifier {
    VERTA_ID,
    EMAIL_ID,
    USER_NAME
  }

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
  String ROLE_PROJECT_DEPLOY = "PROJECT_DEPLOY";
  String ROLE_PROJECT_ADMIN = "PROJECT_ADMIN";
  String ROLE_DATASET_ADMIN = "DATASET_ADMIN";
  String ROLE_EXPERIMENT_OWNER = "EXPERIMENT_OWNER";
  String ROLE_EXPERIMENT_RUN_OWNER = "EXPERIMENT_RUN_OWNER";
  String ROLE_DATASET_VERSION_OWNER = "DATASET_VERSION_OWNER";
  String ROLE_REPOSITORY_OWNER = "REPOSITORY_OWNER";
  String ROLE_REPOSITORY_ADMIN = "REPOSITORY_ADMIN";

  // Telemetry Constants
  String TELEMETRY = "telemetry";
  String OPT_IN = "opt_in";
  String TELEMENTRY_FREQUENCY = "frequency"; // frequency to share data in hours
  String TELEMETRY_CONSUMER = "consumer";
  String TELEMETRY_CONSUMER_URL =
      "https://app.verta.ai/api/v1/uac-proxy/telemetry/collectTelemetry";

  // Versioning constant
  String BLOB = "blob";
  String BLOBS = "blobs";
  String SUBTREES = "subtrees";
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
  String REPOSITORY_VISIBILITY = "repository_visibility";
  String REPOSITORY = "repository";
  String VERSIONING_REPOSITORY = "versioning_repository";
  String VERSIONING_COMMIT = "versioning_commit";
  String VERSIONING_REPO_COMMIT_BLOB = "versioning_repo_commit_blob";
  String VERSIONING_REPO_COMMIT = "versioning_repo_commit";
  String DEFAULT_VERSIONING_BLOB_LOCATION = "version";
  String REPOSITORY_ACCESS_MODIFIER = "repositoryAccessModifier";
  String PROPERTY_NAME = "property_name";
  String S3_PRESIGNED_URL_ENABLED = "s3presignedURLEnabled";

  // Cron job constant
  String FREQUENCY = "frequency"; // frequency to run cron job in second
  String RECORD_UPDATE_LIMIT = "record_update_limit";
  String CRON_JOB = "cron_job";
  String UPDATE_PARENT_TIMESTAMP = "update_parent_timestamp";
  String DELETE_ENTITIES = "delete_entities";
  String BACKGROUND_UTILS_COUNT = "backgroundUtilsCount";
}
