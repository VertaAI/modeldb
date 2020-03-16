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
  String DATABASE = "database";
  String DB_TYPE = "DBType";
  String DISABLED_MDB_COLLABORATOR = "disabled-mdb-collaborator";
  String FEATURE_FLAG = "feature-flag";
  String GET_ARTIFACT_ENDPOINT = "getArtifact";
  String GRPC_SERVER = "grpcServer";
  String HOST = "host";
  String HTTPS_STR = "https";
  String NFS = "NFS";
  String PICK_NFS_HOST_FROM_CONFIG = "pickNFSHostFromConfig";
  String NFS_ROOT_PATH = "nfsRootPath";
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

  // feature-flags
  String DISABLED_AUTHZ = "disabled-authz";
  String STORE_CLIENT_CREATION_TIMESTAMP = "store-client-creation-timestamp";

  // Threshold Constant
  Long DEFAULT_SHUTDOWN_TIMEOUT = 30L; // timeout in second
  Integer NAME_MAX_LENGTH = 40;
  Integer NAME_LENGTH = 256;
  String PATH_DELIMITER = "/";
  Integer TAG_LENGTH = 40;

  // String Constants
  String STRING_COLON = ":";
  String EMPTY_STRING = "";

  // Column/protos field names
  String ARTIFACTS = "artifacts";
  String ATTRIBUTES = "attributes";
  String DATASET_COLLABORATORS = "dataset_collaborators";
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
  String WORKSPACE_NAME = "workspace_name";
  String WORKSPACE_TYPE = "workspace_type";

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
  String TIME_UPDATED = "time_updated";
  String TIME_LOGGED = "time_logged";
  String UNSIGNED_USER = "unsigned_user";
  String VERSION = "version";
  String VERTA_ID_STR = "vertaId";
  String VERTA_ID = "verta_id";
  String EMAIL = "email";
  String USERNAME = "username";

  // Set to true to export the liquibase schema as sql statements
  Boolean EXPORT_SCHEMA = false;

  // Common error messages
  String ACCESS_DENIED_EXPERIMENT_RUN = "User does not have access to the ExperimentRun.";
  String AUTH_SERVICE_CHANNEL_CLOSE_ERROR = "AuthServiceChannel close() error : ";
  String INTERNAL_ERROR = "Internal server error";
  String NON_EQ_ID_PRED_ERROR_MESSAGE =
      "Only equality predicates supported on ids. Use EQ Operator.";

  // Relational Query alias
  String ARTIFACT_ALIAS = "_art_";
  String ATTRIBUTE_ALIAS = "_attr_";
  String DATASET_ALIAS = "_dat_";
  String FEATURE_ALIAS = "ft_";
  String HYPERPARAMETER_ALIAS = "_hypr_";
  String METRICS_ALIAS = "_met_";
  String OBSERVATION_ALIAS = "_ob_";
  String TAGS_ALIAS = "tm_";

  // Migration Constants
  String COLLABORATORS_RBAC_MIGRATION = "collaborators_rbac";
  String MIGRATION = "migration";
  String OWNERS_RBAC_MIGRATION = "owners_rbac";

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
}
