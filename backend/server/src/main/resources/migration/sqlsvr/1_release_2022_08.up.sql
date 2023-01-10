BEGIN TRANSACTION

-- artifact_part definition

-- Drop table

-- DROP TABLE artifact_part;

CREATE TABLE artifact_part (
	artifact_id nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	part_number bigint NOT NULL,
	etag nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	artifact_type int DEFAULT 0 NOT NULL,
	CONSTRAINT PK_ARTIFACT_PART PRIMARY KEY (artifact_type,part_number,artifact_id)
);


-- artifact_store definition

-- Drop table

-- DROP TABLE artifact_store;

CREATE TABLE artifact_store (
	id bigint IDENTITY(1,1) NOT NULL,
	client_key nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	cloud_storage_file_path nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	cloud_storage_key nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	entity_id nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	entity_name nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_ARTIFACT_STORE PRIMARY KEY (id)
);


-- [attribute] definition

-- Drop table

-- DROP TABLE [attribute];

CREATE TABLE [attribute] (
	id bigint IDENTITY(1,1) NOT NULL,
	entity_name nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	field_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	kv_key nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	kv_value nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	value_type int NULL,
	dataset_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	dataset_version_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	experiment_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	experiment_run_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	job_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	project_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	repository_id bigint NULL,
	entity_hash nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_ATTRIBUTE PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX at_d_id ON attribute (  dataset_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX at_dsv_id ON attribute (  dataset_version_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX at_e_id ON attribute (  experiment_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX at_er_id ON attribute (  experiment_run_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX at_j_id ON attribute (  job_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX at_p_id ON attribute (  project_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX attr_field_type ON attribute (  field_type ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX attr_id ON attribute (  id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_entity_hash ON attribute (  entity_hash ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- audit_service_local_audit_log definition

-- Drop table

-- DROP TABLE audit_service_local_audit_log;

CREATE TABLE audit_service_local_audit_log (
	id bigint IDENTITY(1,1) NOT NULL,
	local_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	user_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	[action] int NOT NULL,
	resource_type int NULL,
	resource_service int NULL,
	ts_nano bigint NULL,
	method_name nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	request nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	response nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	workspace_id bigint NULL,
	CONSTRAINT PK_AUDIT_SERVICE_LOCAL_AUDIT_LOG PRIMARY KEY (id),
	CONSTRAINT UQ__audit_se__508901094BC51229 UNIQUE (local_id)
);


-- comment definition

-- Drop table

-- DROP TABLE comment;

CREATE TABLE comment (
	id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	entity_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	entity_name nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_COMMENT PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX c_e_id ON comment (  entity_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- [commit] definition

-- Drop table

-- DROP TABLE [commit];

CREATE TABLE [commit] (
	commit_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	date_created bigint NULL,
	message nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	author nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	root_sha nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	date_updated bigint NULL,
	version_number bigint DEFAULT 1 NULL,
	CONSTRAINT PK_COMMIT PRIMARY KEY (commit_hash)
);
 CREATE NONCLUSTERED INDEX index_commit_hash_date_created ON [commit] (  date_created ASC  , commit_hash ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- database_change_log definition

-- Drop table

-- DROP TABLE database_change_log;

CREATE TABLE database_change_log (
	ID nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	AUTHOR nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	FILENAME nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	DATEEXECUTED datetime2(3) NOT NULL,
	ORDEREXECUTED int NOT NULL,
	EXECTYPE nvarchar(10) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	MD5SUM nvarchar(35) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	DESCRIPTION nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	COMMENTS nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	TAG nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	LIQUIBASE nvarchar(20) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONTEXTS nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	LABELS nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	DEPLOYMENT_ID nvarchar(10) COLLATE SQL_Latin1_General_CP1_CI_AS NULL
);


-- database_change_log_lock definition

-- Drop table

-- DROP TABLE database_change_log_lock;

CREATE TABLE database_change_log_lock (
	ID int NOT NULL,
	LOCKED bit NOT NULL,
	LOCKGRANTED datetime2(3) NULL,
	LOCKEDBY nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_DATABASE_CHANGE_LOG_LOCK PRIMARY KEY (ID)
);


-- dataset definition

-- Drop table

-- DROP TABLE dataset;

CREATE TABLE dataset (
	id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	dataset_type int NULL,
	dataset_visibility int NULL,
	description nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	name nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	owner nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	time_created bigint NULL,
	time_updated bigint NULL,
	workspace nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	workspace_type int NULL,
	deleted bit DEFAULT 0 NULL,
	workspace_id bigint NULL,
	CONSTRAINT PK_DATASET PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX d_name_wspace_wspace_type_deleted ON dataset (  workspace ASC  , deleted ASC  , name ASC  , workspace_type ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_dataset_deleted ON dataset (  deleted ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_dataset_id_deleted ON dataset (  deleted ASC  , id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_dataset_id_time_updated ON dataset (  id ASC  , time_updated ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_dataset_time_updated ON dataset (  time_updated ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- docker_environment_blob definition

-- Drop table

-- DROP TABLE docker_environment_blob;

CREATE TABLE docker_environment_blob (
	blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	repository nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	tag nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	sha nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_DOCKER_ENVIRONMENT_BLOB PRIMARY KEY (blob_hash)
);


-- event definition

-- Drop table

-- DROP TABLE event;

CREATE TABLE event (
	event_uuid nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	event_type nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	workspace_id bigint NULL,
	event_metadata nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT pk_query_event_uuid PRIMARY KEY (event_uuid)
);


-- folder_element definition

-- Drop table

-- DROP TABLE folder_element;

CREATE TABLE folder_element (
	folder_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	element_sha nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	element_name nvarchar(200) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	element_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_FOLDER_ELEMENT PRIMARY KEY (folder_hash,element_name)
);


-- git_code_blob definition

-- Drop table

-- DROP TABLE git_code_blob;

CREATE TABLE git_code_blob (
	blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	repo nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	commit_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	branch nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	tag nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	is_dirty bit NULL,
	CONSTRAINT PK_GIT_CODE_BLOB PRIMARY KEY (blob_hash)
);


-- git_snapshot definition

-- Drop table

-- DROP TABLE git_snapshot;

CREATE TABLE git_snapshot (
	id bigint IDENTITY(1,1) NOT NULL,
	field_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	hash nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	is_dirty int NULL,
	repo nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_GIT_SNAPSHOT PRIMARY KEY (id)
);


-- hyperparameter_element_config_blob definition

-- Drop table

-- DROP TABLE hyperparameter_element_config_blob;

CREATE TABLE hyperparameter_element_config_blob (
	blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	name nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	commit_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	value_type int NULL,
	int_value bigint NULL,
	float_value float NULL,
	string_value nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_HYPERPARAMETER_ELEMENT_CONFIG_BLOB PRIMARY KEY (blob_hash)
);


-- job definition

-- Drop table

-- DROP TABLE job;

CREATE TABLE job (
	id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	description nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	end_time nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	job_status int NULL,
	job_type int NULL,
	owner nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	start_time nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_JOB PRIMARY KEY (id)
);


-- key_value_property_mapping definition

-- Drop table

-- DROP TABLE key_value_property_mapping;

CREATE TABLE key_value_property_mapping (
	entity_hash nvarchar(256) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	property_name nvarchar(256) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	kv_key nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	kv_value nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_KEY_VALUE_PROPERTY_MAPPING PRIMARY KEY (entity_hash,kv_key,property_name)
);


-- labels_mapping definition

-- Drop table

-- DROP TABLE labels_mapping;

CREATE TABLE labels_mapping (
	entity_hash nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	entity_type int NOT NULL,
	label nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	CONSTRAINT pk_label_mapping PRIMARY KEY (entity_type,entity_hash,label)
);
 CREATE NONCLUSTERED INDEX index_labels_mapping_entity_hash_entity_type ON labels_mapping (  entity_type ASC  , entity_hash ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_labels_mapping_label ON labels_mapping (  label ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- lineage definition

-- Drop table

-- DROP TABLE lineage;

CREATE TABLE lineage (
	input_external_id nvarchar(256) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	input_type int NOT NULL,
	output_external_id nvarchar(256) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	output_type int NOT NULL,
	CONSTRAINT PK_LINEAGE PRIMARY KEY (output_type,input_type,output_external_id,input_external_id)
);
 CREATE NONCLUSTERED INDEX p_input_lineage ON lineage (  input_type ASC  , input_external_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX p_output_lineage ON lineage (  output_type ASC  , output_external_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- metadata_property_mapping definition

-- Drop table

-- DROP TABLE metadata_property_mapping;

CREATE TABLE metadata_property_mapping (
	repository_id bigint NOT NULL,
	commit_sha nvarchar(256) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	location nvarchar(256) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	metadata_key nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	metadata_value nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_METADATA_PROPERTY_MAPPING PRIMARY KEY (commit_sha,metadata_key,repository_id,location)
);


-- migration_status definition

-- Drop table

-- DROP TABLE migration_status;

CREATE TABLE migration_status (
	id bigint IDENTITY(1,1) NOT NULL,
	migration_name nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	status tinyint NULL,
	CONSTRAINT PK_MIGRATION_STATUS PRIMARY KEY (id)
);


-- modeldb_deployment_info definition

-- Drop table

-- DROP TABLE modeldb_deployment_info;

CREATE TABLE modeldb_deployment_info (
	md_key nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	md_value nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	creation_timestamp bigint NULL
);


-- path_dataset_component_blob definition

-- Drop table

-- DROP TABLE path_dataset_component_blob;

CREATE TABLE path_dataset_component_blob (
	path_dataset_blob_id nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	last_modified_at_source bigint NULL,
	md5 nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[path] nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	sha256 nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[size] bigint NULL,
	internal_versioned_path nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	base_path nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT pk_path_dataset_component PRIMARY KEY (path_dataset_blob_id,blob_hash)
);


-- path_dataset_version_info definition

-- Drop table

-- DROP TABLE path_dataset_version_info;

CREATE TABLE path_dataset_version_info (
	id bigint IDENTITY(1,1) NOT NULL,
	base_path nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	field_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	location_type int NULL,
	[size] bigint NULL,
	CONSTRAINT PK_PATH_DATASET_VERSION_INFO PRIMARY KEY (id)
);


-- python_environment_blob definition

-- Drop table

-- DROP TABLE python_environment_blob;

CREATE TABLE python_environment_blob (
	blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	major int NULL,
	minor int NULL,
	patch int NULL,
	suffix nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	raw_requirements nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	raw_constraints nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_PYTHON_ENVIRONMENT_BLOB PRIMARY KEY (blob_hash)
);


-- query_dataset_component_blob definition

-- Drop table

-- DROP TABLE query_dataset_component_blob;

CREATE TABLE query_dataset_component_blob (
	query_dataset_blob_id nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	query nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	data_source_uri nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	execution_timestamp bigint NULL,
	num_records bigint NULL,
	CONSTRAINT pk_query_dataset_component PRIMARY KEY (blob_hash,query_dataset_blob_id)
);


-- query_dataset_version_info definition

-- Drop table

-- DROP TABLE query_dataset_version_info;

CREATE TABLE query_dataset_version_info (
	id bigint IDENTITY(1,1) NOT NULL,
	data_source_uri nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	execution_timestamp bigint NULL,
	field_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	num_records bigint NULL,
	query nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	query_template nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_QUERY_DATASET_VERSION_INFO PRIMARY KEY (id)
);


-- raw_dataset_version_info definition

-- Drop table

-- DROP TABLE raw_dataset_version_info;

CREATE TABLE raw_dataset_version_info (
	id bigint IDENTITY(1,1) NOT NULL,
	checksum nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	field_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	num_records bigint NULL,
	object_path nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[size] bigint NULL,
	CONSTRAINT PK_RAW_DATASET_VERSION_INFO PRIMARY KEY (id)
);


-- repository definition

-- Drop table

-- DROP TABLE repository;

CREATE TABLE repository (
	id bigint IDENTITY(1,1) NOT NULL,
	date_created bigint NULL,
	date_updated bigint NULL,
	name nvarchar(256) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	workspace_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	workspace_type int NULL,
	owner nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	repository_visibility int NULL,
	repository_access_modifier int DEFAULT 1 NULL,
	description nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	deleted bit DEFAULT 0 NULL,
	workspace_service_id bigint NULL,
	created bit DEFAULT 0 NULL,
	visibility_migration bit DEFAULT 0 NULL,
	version_number bigint DEFAULT 1 NULL,
	CONSTRAINT PK_REPOSITORY PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX index_repo_date_updated ON repository (  date_updated ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_repo_id_date_updated ON repository (  date_updated ASC  , id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_repository_deleted ON repository (  deleted ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_repository_id_deleted ON repository (  deleted ASC  , id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_visibility_migration_repository ON repository (  visibility_migration ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- s3_dataset_component_blob definition

-- Drop table

-- DROP TABLE s3_dataset_component_blob;

CREATE TABLE s3_dataset_component_blob (
	s3_dataset_blob_id nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	last_modified_at_source bigint NULL,
	md5 nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[path] nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	sha256 nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[size] bigint NULL,
	s3_version_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	internal_versioned_path nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	base_path nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT pk_s3_dataset_component PRIMARY KEY (s3_dataset_blob_id,blob_hash)
);


-- telemetry_information definition

-- Drop table

-- DROP TABLE telemetry_information;

CREATE TABLE telemetry_information (
	tel_key nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	tel_value nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	collection_timestamp bigint NULL,
	transfer_timestamp bigint NULL,
	telemetry_consumer nvarchar(256) COLLATE SQL_Latin1_General_CP1_CI_AS NULL
);


-- upload_status definition

-- Drop table

-- DROP TABLE upload_status;

CREATE TABLE upload_status (
	id bigint IDENTITY(1,1) NOT NULL,
	dataset_component_blob_id nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	component_blob_type int NULL,
	upload_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS DEFAULT NULL NULL,
	upload_completed bit DEFAULT 1 NULL,
	CONSTRAINT PK_UPLOAD_STATUS PRIMARY KEY (id)
);


-- versioning_modeldb_entity_mapping definition

-- Drop table

-- DROP TABLE versioning_modeldb_entity_mapping;

CREATE TABLE versioning_modeldb_entity_mapping (
	repository_id bigint NULL,
	[commit] nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	versioning_key nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	versioning_location nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	experiment_run_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	entity_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	versioning_blob_type int NULL,
	blob_hash nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT ck_versioning_modeldb_entity_mapping UNIQUE (entity_type,versioning_key,[commit],repository_id,experiment_run_id)
);
 CREATE NONCLUSTERED INDEX index_vmem_exp_run_id ON versioning_modeldb_entity_mapping (  experiment_run_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_vmem_repo_id_commit ON versioning_modeldb_entity_mapping (  [commit] ASC  , repository_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_vmem_table ON versioning_modeldb_entity_mapping (  versioning_blob_type ASC  , [commit] ASC  , repository_id ASC  , experiment_run_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_vmem_versioning_blob_type ON versioning_modeldb_entity_mapping (  versioning_blob_type ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- versioning_modeldb_entity_mapping_config_blob definition

-- Drop table

-- DROP TABLE versioning_modeldb_entity_mapping_config_blob;

CREATE TABLE versioning_modeldb_entity_mapping_config_blob (
	versioning_modeldb_entity_mapping_repository_id bigint NULL,
	versioning_modeldb_entity_mapping_commit nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	versioning_modeldb_entity_mapping_versioning_key nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	versioning_modeldb_entity_mapping_experiment_run_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	versioning_modeldb_entity_mapping_entity_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	config_blob_entity_blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	config_blob_entity_config_seq_number int NULL
);


-- audit_resource_workspace_mapping definition

-- Drop table

-- DROP TABLE audit_resource_workspace_mapping;

CREATE TABLE audit_resource_workspace_mapping (
	audit_log_id bigint NULL,
	resource_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	workspace_id bigint NULL,
	CONSTRAINT audit_fk_audit_service_local_audit_log_id FOREIGN KEY (audit_log_id) REFERENCES audit_service_local_audit_log(id)
);


-- branch definition

-- Drop table

-- DROP TABLE branch;

CREATE TABLE branch (
	branch nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	commit_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	repository_id bigint NOT NULL,
	CONSTRAINT pk_branch PRIMARY KEY (repository_id,branch),
	CONSTRAINT fk_commit_hash_branchs FOREIGN KEY (commit_hash) REFERENCES [commit](commit_hash),
	CONSTRAINT fk_repository_id_branchs FOREIGN KEY (repository_id) REFERENCES repository(id)
);


-- commit_parent definition

-- Drop table

-- DROP TABLE commit_parent;

CREATE TABLE commit_parent (
	parent_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	child_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	parent_order int NULL,
	CONSTRAINT fk_child_hash_commit_parent FOREIGN KEY (child_hash) REFERENCES [commit](commit_hash),
	CONSTRAINT fk_parent_hash_commit_parent FOREIGN KEY (parent_hash) REFERENCES [commit](commit_hash)
);


-- dataset_part_info definition

-- Drop table

-- DROP TABLE dataset_part_info;

CREATE TABLE dataset_part_info (
	id bigint IDENTITY(1,1) NOT NULL,
	checksum nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	entity_name nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	field_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	last_modified_at_source bigint NULL,
	[path] nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[size] bigint NULL,
	path_dataset_version_info_id bigint NULL,
	CONSTRAINT PK_DATASET_PART_INFO PRIMARY KEY (id),
	CONSTRAINT [createTable-9_fk_path_dataset_version_info_id] FOREIGN KEY (path_dataset_version_info_id) REFERENCES path_dataset_version_info(id)
);
 CREATE NONCLUSTERED INDEX dsp_pdsv_id ON dataset_part_info (  path_dataset_version_info_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- dataset_repository_mapping definition

-- Drop table

-- DROP TABLE dataset_repository_mapping;

CREATE TABLE dataset_repository_mapping (
	repository_id bigint NOT NULL,
	CONSTRAINT PK_DATASET_REPOSITORY_MAPPING PRIMARY KEY (repository_id),
	CONSTRAINT fk_repository_id FOREIGN KEY (repository_id) REFERENCES repository(id)
);


-- dataset_version definition

-- Drop table

-- DROP TABLE dataset_version;

CREATE TABLE dataset_version (
	id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	dataset_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	dataset_type int NULL,
	dataset_version_visibility int NULL,
	description nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	owner nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	parent_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	time_logged bigint NULL,
	time_updated bigint NULL,
	version bigint NULL,
	path_dataset_version_info_id bigint NULL,
	query_dataset_version_info_id bigint NULL,
	raw_dataset_version_info_id bigint NULL,
	deleted bit DEFAULT 0 NULL,
	CONSTRAINT PK_DATASET_VERSION PRIMARY KEY (id),
	CONSTRAINT datasetversion_fk_path_dataset_version_info_id FOREIGN KEY (path_dataset_version_info_id) REFERENCES path_dataset_version_info(id),
	CONSTRAINT datasetversion_fk_query_dataset_version_info_id FOREIGN KEY (query_dataset_version_info_id) REFERENCES query_dataset_version_info(id),
	CONSTRAINT datasetversion_fk_raw_dataset_version_info_id FOREIGN KEY (raw_dataset_version_info_id) REFERENCES raw_dataset_version_info(id)
);
 CREATE NONCLUSTERED INDEX dsv_pdsv_id ON dataset_version (  path_dataset_version_info_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX dsv_qdsv_id ON dataset_version (  query_dataset_version_info_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX dsv_rdsv_id ON dataset_version (  raw_dataset_version_info_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_dataset_version_dataset_id_version_deleted ON dataset_version (  deleted ASC  , dataset_id ASC  , version ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_dataset_version_deleted ON dataset_version (  deleted ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_dataset_version_id_deleted ON dataset_version (  deleted ASC  , id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_dv_dataset_id ON dataset_version (  dataset_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_dv_dataset_id_time_updated ON dataset_version (  dataset_id ASC  , time_updated ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- environment_blob definition

-- Drop table

-- DROP TABLE environment_blob;

CREATE TABLE environment_blob (
	blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	environment_type int NULL,
	python_environment_blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	docker_environment_blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_ENVIRONMENT_BLOB PRIMARY KEY (blob_hash),
	CONSTRAINT fk_docker_environment_blob FOREIGN KEY (docker_environment_blob_hash) REFERENCES docker_environment_blob(blob_hash),
	CONSTRAINT fk_python_environment_blob FOREIGN KEY (python_environment_blob_hash) REFERENCES python_environment_blob(blob_hash)
);


-- environment_command_line definition

-- Drop table

-- DROP TABLE environment_command_line;

CREATE TABLE environment_command_line (
	environment_blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	command_seq_number int NOT NULL,
	command nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_ENVIRONMENT_COMMAND_LINE PRIMARY KEY (command_seq_number,environment_blob_hash),
	CONSTRAINT fk_environment_blob_hash FOREIGN KEY (environment_blob_hash) REFERENCES environment_blob(blob_hash)
);


-- environment_variables definition

-- Drop table

-- DROP TABLE environment_variables;

CREATE TABLE environment_variables (
	environment_blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	variable_name nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	variable_value nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_ENVIRONMENT_VARIABLES PRIMARY KEY (environment_blob_hash,variable_name),
	CONSTRAINT fk_environment_blob FOREIGN KEY (environment_blob_hash) REFERENCES environment_blob(blob_hash)
);


-- git_snapshot_file_paths definition

-- Drop table

-- DROP TABLE git_snapshot_file_paths;

CREATE TABLE git_snapshot_file_paths (
	git_snapshot_id bigint NULL,
	file_paths nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT fk_gitsnapshotentity_id FOREIGN KEY (git_snapshot_id) REFERENCES git_snapshot(id)
);
 CREATE NONCLUSTERED INDEX gfp_g_id ON git_snapshot_file_paths (  git_snapshot_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- hyperparameter_set_config_blob definition

-- Drop table

-- DROP TABLE hyperparameter_set_config_blob;

CREATE TABLE hyperparameter_set_config_blob (
	blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	name nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	value_type int NULL,
	interval_begin_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	interval_end_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	interval_step_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_HYPERPARAMETER_SET_CONFIG_BLOB PRIMARY KEY (blob_hash),
	CONSTRAINT fk_begin_hyperparameter_element_config_blob FOREIGN KEY (interval_begin_hash) REFERENCES hyperparameter_element_config_blob(blob_hash),
	CONSTRAINT fk_end_hyperparameter_element_config_blob FOREIGN KEY (interval_end_hash) REFERENCES hyperparameter_element_config_blob(blob_hash),
	CONSTRAINT fk_step_hyperparameter_element_config_blob FOREIGN KEY (interval_step_hash) REFERENCES hyperparameter_element_config_blob(blob_hash)
);


-- notebook_code_blob definition

-- Drop table

-- DROP TABLE notebook_code_blob;

CREATE TABLE notebook_code_blob (
	blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	git_code_blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	path_dataset_blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_NOTEBOOK_CODE_BLOB PRIMARY KEY (blob_hash),
	CONSTRAINT fk_git_code_blob FOREIGN KEY (git_code_blob_hash) REFERENCES git_code_blob(blob_hash)
);


-- python_environment_requirements_blob definition

-- Drop table

-- DROP TABLE python_environment_requirements_blob;

CREATE TABLE python_environment_requirements_blob (
	python_environment_blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	library nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	python_constraint nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	major int NULL,
	minor int NULL,
	patch int NULL,
	suffix nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	req_or_constraint bit NOT NULL,
	CONSTRAINT PK_PYTHON_ENVIRONMENT_REQUIREMENTS_BLOB PRIMARY KEY (req_or_constraint,library,python_constraint,python_environment_blob_hash),
	CONSTRAINT fk_perb_python_environment_blob FOREIGN KEY (python_environment_blob_hash) REFERENCES python_environment_blob(blob_hash)
);


-- query_parameter definition

-- Drop table

-- DROP TABLE query_parameter;

CREATE TABLE query_parameter (
	id bigint IDENTITY(1,1) NOT NULL,
	entity_name nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	field_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	parameter_name nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	parameter_type int NULL,
	parameter_value nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	query_dataset_version_info_id bigint NULL,
	CONSTRAINT PK_QUERY_PARAMETER PRIMARY KEY (id),
	CONSTRAINT query_parameter_fk_query_dataset_version_info_id FOREIGN KEY (query_dataset_version_info_id) REFERENCES query_dataset_version_info(id)
);
 CREATE NONCLUSTERED INDEX qp_qdsv_id ON query_parameter (  query_dataset_version_info_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- repository_commit definition

-- Drop table

-- DROP TABLE repository_commit;

CREATE TABLE repository_commit (
	repository_id bigint NULL,
	commit_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT fk_entity_id_repository_entity_mapping FOREIGN KEY (commit_hash) REFERENCES [commit](commit_hash),
	CONSTRAINT fk_repository_id_commit_hash_mapping FOREIGN KEY (repository_id) REFERENCES repository(id)
);
 CREATE NONCLUSTERED INDEX index_repository_commit_repo_id ON repository_commit (  repository_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- tag definition

-- Drop table

-- DROP TABLE tag;

CREATE TABLE tag (
	tag nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	commit_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	repository_id bigint NOT NULL,
	CONSTRAINT pk_tag PRIMARY KEY (repository_id,tag),
	CONSTRAINT fk_commit_hash_tags FOREIGN KEY (commit_hash) REFERENCES [commit](commit_hash),
	CONSTRAINT fk_repository_id_tags FOREIGN KEY (repository_id) REFERENCES repository(id)
);


-- user_comment definition

-- Drop table

-- DROP TABLE user_comment;

CREATE TABLE user_comment (
	id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	date_time bigint NULL,
	entity_name nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	message nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	user_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	comment_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	owner nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_USER_COMMENT PRIMARY KEY (id),
	CONSTRAINT fk_comment_id FOREIGN KEY (comment_id) REFERENCES comment(id)
);
 CREATE NONCLUSTERED INDEX uc_c_id ON user_comment (  comment_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- config_blob definition

-- Drop table

-- DROP TABLE config_blob;

CREATE TABLE config_blob (
	blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	config_seq_number int NOT NULL,
	hyperparameter_type int NULL,
	hyperparameter_set_config_blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	hyperparameter_element_config_blob_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT pk_config_blob PRIMARY KEY (config_seq_number,blob_hash),
	CONSTRAINT fk_cb_hyperparameter_element_config_blob FOREIGN KEY (hyperparameter_element_config_blob_hash) REFERENCES hyperparameter_element_config_blob(blob_hash),
	CONSTRAINT fk_cb_hyperparameter_set_config_blob FOREIGN KEY (hyperparameter_set_config_blob_hash) REFERENCES hyperparameter_set_config_blob(blob_hash)
);


-- hyperparameter_discrete_set_element_mapping definition

-- Drop table

-- DROP TABLE hyperparameter_discrete_set_element_mapping;

CREATE TABLE hyperparameter_discrete_set_element_mapping (
	set_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	element_hash nvarchar(64) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	CONSTRAINT PK_HYPERPARAMETER_DISCRETE_SET_ELEMENT_MAPPING PRIMARY KEY (set_hash,element_hash),
	CONSTRAINT fk_hyperparameter_element_config_blob FOREIGN KEY (element_hash) REFERENCES hyperparameter_element_config_blob(blob_hash),
	CONSTRAINT fk_hyperparameter_set_config_blob FOREIGN KEY (set_hash) REFERENCES hyperparameter_set_config_blob(blob_hash)
);


-- artifact definition

-- Drop table

-- DROP TABLE artifact;

CREATE TABLE artifact (
	id bigint IDENTITY(1,1) NOT NULL,
	artifact_type int NULL,
	entity_name nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	field_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	filename_extension nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	ar_key nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	linked_artifact_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	ar_path nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	path_only bit NULL,
	experiment_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	experiment_run_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	project_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	store_type_path nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	upload_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS DEFAULT NULL NULL,
	upload_completed bit DEFAULT 1 NULL,
	serialization nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	artifact_subtype nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_ARTIFACT PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX a_e_id ON artifact (  experiment_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX a_er_id ON artifact (  experiment_run_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX a_l_a_id ON artifact (  linked_artifact_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX a_p_id ON artifact (  project_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- code_version definition

-- Drop table

-- DROP TABLE code_version;

CREATE TABLE code_version (
	id bigint IDENTITY(1,1) NOT NULL,
	date_logged bigint NULL,
	field_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	code_archive_id bigint NULL,
	git_snapshot_id bigint NULL,
	CONSTRAINT PK_CODE_VERSION PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX cv_ca_id ON code_version (  code_archive_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX cv_gss_id ON code_version (  git_snapshot_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- experiment definition

-- Drop table

-- DROP TABLE experiment;

CREATE TABLE experiment (
	id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	date_created bigint NULL,
	date_updated bigint NULL,
	description nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	name nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	owner nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	project_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	code_version_snapshot_id bigint NULL,
	deleted bit DEFAULT 0 NULL,
	version_number bigint DEFAULT 1 NULL,
	created bit DEFAULT 0 NULL,
	CONSTRAINT PK_EXPERIMENT PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX e_cvs_id ON experiment (  code_version_snapshot_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_exp_date_updated ON experiment (  date_updated ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_exp_name_project_id ON experiment (  project_id ASC  , name ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_exp_project_id ON experiment (  project_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_experiment_deleted ON experiment (  deleted ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_experiment_id_deleted ON experiment (  deleted ASC  , id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_project_id_date_updated ON experiment (  date_updated ASC  , project_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- experiment_run definition

-- Drop table

-- DROP TABLE experiment_run;

CREATE TABLE experiment_run (
	id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	code_version nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	date_created bigint NULL,
	date_updated bigint NULL,
	description nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	end_time bigint NULL,
	experiment_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	job_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	name nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	owner nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	parent_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	project_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	start_time bigint NULL,
	code_version_snapshot_id bigint NULL,
	deleted bit DEFAULT 0 NULL,
	environment nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	created bit DEFAULT 0 NULL,
	version_number bigint DEFAULT 1 NULL,
	CONSTRAINT PK_EXPERIMENT_RUN PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX er_cvs_id ON experiment_run (  code_version_snapshot_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX er_dc ON experiment_run (  date_created ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX er_dp ON experiment_run (  date_updated ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX er_experiment_id ON experiment_run (  experiment_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX er_n ON experiment_run (  name ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX er_o ON experiment_run (  owner ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX er_p_id ON experiment_run (  project_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_exp_run_exp_id_date_updated ON experiment_run (  date_updated ASC  , experiment_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_exp_run_id_project_id ON experiment_run (  project_id ASC  , id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_exp_run_name_project_id ON experiment_run (  project_id ASC  , name ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_exp_run_project_id_experiment_id ON experiment_run (  project_id ASC  , experiment_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_experiment_run_deleted ON experiment_run (  deleted ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_experiment_run_id_deleted ON experiment_run (  deleted ASC  , id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_experiment_run_name_pro_id_exp_id_deleted ON experiment_run (  deleted ASC  , project_id ASC  , experiment_id ASC  , name ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- feature definition

-- Drop table

-- DROP TABLE feature;

CREATE TABLE feature (
	id bigint IDENTITY(1,1) NOT NULL,
	entity_name nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	feature nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	experiment_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	experiment_run_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	project_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	raw_dataset_version_info_id bigint NULL,
	CONSTRAINT PK_FEATURE PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX f_e_id ON feature (  experiment_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX f_er_id ON feature (  experiment_run_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX f_p_id ON feature (  project_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX f_rdsv_id ON feature (  raw_dataset_version_info_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- hyperparameter_element_mapping definition

-- Drop table

-- DROP TABLE hyperparameter_element_mapping;

CREATE TABLE hyperparameter_element_mapping (
	id bigint IDENTITY(1,1) NOT NULL,
	experiment_run_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	name nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	entity_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	int_value bigint NULL,
	float_value float NULL,
	string_value nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_HYPERPARAMETER_ELEMENT_MAPPING PRIMARY KEY (id)
);


-- keyvalue definition

-- Drop table

-- DROP TABLE keyvalue;

CREATE TABLE keyvalue (
	id bigint IDENTITY(1,1) NOT NULL,
	entity_name nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	field_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	kv_key nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	kv_value nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	value_type int NULL,
	dataset_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	dataset_version_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	experiment_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	experiment_run_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	job_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	project_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_KEYVALUE PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX kv_d_id ON keyvalue (  dataset_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX kv_dsv_id ON keyvalue (  dataset_version_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX kv_e_id ON keyvalue (  experiment_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX kv_er_id ON keyvalue (  experiment_run_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX kv_field_type ON keyvalue (  field_type ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX kv_j_id ON keyvalue (  job_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX kv_p_id ON keyvalue (  project_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- observation definition

-- Drop table

-- DROP TABLE observation;

CREATE TABLE observation (
	id bigint IDENTITY(1,1) NOT NULL,
	entity_name nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	field_type nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[timestamp] AS 10000+id,
	artifact_id bigint NULL,
	experiment_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	experiment_run_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	keyvaluemapping_id bigint NULL,
	project_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	epoch_number bigint NULL,
	CONSTRAINT PK_OBSERVATION PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX o_a_id ON observation (  artifact_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX o_e_id ON observation (  experiment_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX o_er_id ON observation (  experiment_run_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX o_kv_id ON observation (  keyvaluemapping_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX o_p_id ON observation (  project_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- project definition

-- Drop table

-- DROP TABLE project;

CREATE TABLE project (
	id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	date_created bigint NULL,
	date_updated bigint NULL,
	description nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	name nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	owner nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	project_visibility int NULL,
	readme_text nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	short_name nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	code_version_snapshot_id bigint NULL,
	workspace nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	workspace_type int NULL,
	deleted bit DEFAULT 0 NULL,
	workspace_id bigint NULL,
	created bit DEFAULT 0 NULL,
	visibility_migration bit DEFAULT 0 NULL,
	version_number bigint DEFAULT 1 NULL,
	CONSTRAINT PK_PROJECT PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX index_project_date_updated ON project (  date_updated ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_project_deleted ON project (  deleted ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_project_id_deleted ON project (  deleted ASC  , id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_project_p_id_date_updated ON project (  date_updated ASC  , id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX index_visibility_migration_project ON project (  visibility_migration ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX p_cvs_id ON project (  code_version_snapshot_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX p_name ON project (  name ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX p_name_wspace_wspace_type_deleted ON project (  workspace ASC  , deleted ASC  , name ASC  , workspace_type ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- tag_mapping definition

-- Drop table

-- DROP TABLE tag_mapping;

CREATE TABLE tag_mapping (
	id bigint IDENTITY(1,1) NOT NULL,
	entity_name nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	tags nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	dataset_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	dataset_version_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	experiment_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	experiment_run_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	project_id nvarchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	CONSTRAINT PK_TAG_MAPPING PRIMARY KEY (id)
);
 CREATE NONCLUSTERED INDEX t_ds_id ON tag_mapping (  dataset_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX t_dsv_id ON tag_mapping (  dataset_version_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX t_e_id ON tag_mapping (  experiment_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX t_er_id ON tag_mapping (  experiment_run_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX t_p_id ON tag_mapping (  project_id ASC  )
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- artifact foreign keys

ALTER TABLE artifact ADD CONSTRAINT artifact_fk_experiment_id FOREIGN KEY (experiment_id) REFERENCES experiment(id);
ALTER TABLE artifact ADD CONSTRAINT artifact_fk_experiment_run_id FOREIGN KEY (experiment_run_id) REFERENCES experiment_run(id);
ALTER TABLE artifact ADD CONSTRAINT artifact_fk_project_id FOREIGN KEY (project_id) REFERENCES project(id);


-- code_version foreign keys

ALTER TABLE code_version ADD CONSTRAINT fk_code_archive_id FOREIGN KEY (code_archive_id) REFERENCES artifact(id);
ALTER TABLE code_version ADD CONSTRAINT fk_git_snapshot_id FOREIGN KEY (git_snapshot_id) REFERENCES git_snapshot(id);


-- experiment foreign keys

ALTER TABLE experiment ADD CONSTRAINT experiment_fk_code_version_snapshot_id FOREIGN KEY (code_version_snapshot_id) REFERENCES code_version(id);


-- experiment_run foreign keys

ALTER TABLE experiment_run ADD CONSTRAINT experimentrun_fk_code_version_snapshot_id FOREIGN KEY (code_version_snapshot_id) REFERENCES code_version(id);


-- feature foreign keys

ALTER TABLE feature ADD CONSTRAINT feature_fk_experiment_id FOREIGN KEY (experiment_id) REFERENCES experiment(id);
ALTER TABLE feature ADD CONSTRAINT feature_fk_experiment_run_id FOREIGN KEY (experiment_run_id) REFERENCES experiment_run(id);
ALTER TABLE feature ADD CONSTRAINT feature_fk_project_id FOREIGN KEY (project_id) REFERENCES project(id);
ALTER TABLE feature ADD CONSTRAINT feature_fk_raw_dataset_version_info_id FOREIGN KEY (raw_dataset_version_info_id) REFERENCES raw_dataset_version_info(id);


-- hyperparameter_element_mapping foreign keys

ALTER TABLE hyperparameter_element_mapping ADD CONSTRAINT hem_fk_experiment_run_id FOREIGN KEY (experiment_run_id) REFERENCES experiment_run(id);


-- keyvalue foreign keys

ALTER TABLE keyvalue ADD CONSTRAINT keyvalue_fk_dataset_id FOREIGN KEY (dataset_id) REFERENCES dataset(id);
ALTER TABLE keyvalue ADD CONSTRAINT keyvalue_fk_dataset_version_id FOREIGN KEY (dataset_version_id) REFERENCES dataset_version(id);
ALTER TABLE keyvalue ADD CONSTRAINT keyvalue_fk_experiment_id FOREIGN KEY (experiment_id) REFERENCES experiment(id);
ALTER TABLE keyvalue ADD CONSTRAINT keyvalue_fk_experiment_run_id FOREIGN KEY (experiment_run_id) REFERENCES experiment_run(id);
ALTER TABLE keyvalue ADD CONSTRAINT keyvalue_fk_job_id FOREIGN KEY (job_id) REFERENCES job(id);
ALTER TABLE keyvalue ADD CONSTRAINT keyvalue_fk_project_id FOREIGN KEY (project_id) REFERENCES project(id);


-- observation foreign keys

ALTER TABLE observation ADD CONSTRAINT observation_fk_artifact_id FOREIGN KEY (artifact_id) REFERENCES artifact(id);
ALTER TABLE observation ADD CONSTRAINT observation_fk_experiment_id FOREIGN KEY (experiment_id) REFERENCES experiment(id);
ALTER TABLE observation ADD CONSTRAINT observation_fk_experiment_run_id FOREIGN KEY (experiment_run_id) REFERENCES experiment_run(id);
ALTER TABLE observation ADD CONSTRAINT observation_fk_keyvaluemapping_id FOREIGN KEY (keyvaluemapping_id) REFERENCES keyvalue(id);
ALTER TABLE observation ADD CONSTRAINT observation_fk_project_id FOREIGN KEY (project_id) REFERENCES project(id);


-- project foreign keys

ALTER TABLE project ADD CONSTRAINT project_fk_code_version_snapshot_id FOREIGN KEY (code_version_snapshot_id) REFERENCES code_version(id);


-- tag_mapping foreign keys

ALTER TABLE tag_mapping ADD CONSTRAINT fk_project_id FOREIGN KEY (project_id) REFERENCES project(id);
ALTER TABLE tag_mapping ADD CONSTRAINT tagmapping_fk_dataset_id FOREIGN KEY (dataset_id) REFERENCES dataset(id);
ALTER TABLE tag_mapping ADD CONSTRAINT tagmapping_fk_dataset_version_id FOREIGN KEY (dataset_version_id) REFERENCES dataset_version(id);
ALTER TABLE tag_mapping ADD CONSTRAINT tagmapping_fk_experiment_id FOREIGN KEY (experiment_id) REFERENCES experiment(id);
ALTER TABLE tag_mapping ADD CONSTRAINT tagmapping_fk_experiment_run_id FOREIGN KEY (experiment_run_id) REFERENCES experiment_run(id);

COMMIT TRANSACTION