BEGIN TRANSACTION

-- artifact foreign keys

ALTER TABLE artifact DROP CONSTRAINT artifact_fk_experiment_id
ALTER TABLE artifact DROP CONSTRAINT artifact_fk_experiment_run_id
ALTER TABLE artifact DROP CONSTRAINT artifact_fk_project_id


-- code_version foreign keys

ALTER TABLE code_version DROP CONSTRAINT fk_code_archive_id
ALTER TABLE code_version DROP CONSTRAINT fk_git_snapshot_id


-- experiment foreign keys

ALTER TABLE experiment DROP CONSTRAINT experiment_fk_code_version_snapshot_id


-- experiment_run foreign keys

ALTER TABLE experiment_run DROP CONSTRAINT experimentrun_fk_code_version_snapshot_id


-- feature foreign keys

ALTER TABLE feature DROP CONSTRAINT feature_fk_experiment_id
ALTER TABLE feature DROP CONSTRAINT feature_fk_experiment_run_id
ALTER TABLE feature DROP CONSTRAINT feature_fk_project_id
ALTER TABLE feature DROP CONSTRAINT feature_fk_raw_dataset_version_info_id


-- hyperparameter_element_mapping foreign keys

ALTER TABLE hyperparameter_element_mapping DROP CONSTRAINT hem_fk_experiment_run_id


-- keyvalue foreign keys

ALTER TABLE keyvalue DROP CONSTRAINT keyvalue_fk_dataset_id
ALTER TABLE keyvalue DROP CONSTRAINT keyvalue_fk_dataset_version_id
ALTER TABLE keyvalue DROP CONSTRAINT keyvalue_fk_experiment_id
ALTER TABLE keyvalue DROP CONSTRAINT keyvalue_fk_experiment_run_id
ALTER TABLE keyvalue DROP CONSTRAINT keyvalue_fk_job_id
ALTER TABLE keyvalue DROP CONSTRAINT keyvalue_fk_project_id


-- observation foreign keys

ALTER TABLE observation DROP CONSTRAINT observation_fk_artifact_id
ALTER TABLE observation DROP CONSTRAINT observation_fk_experiment_id
ALTER TABLE observation DROP CONSTRAINT observation_fk_experiment_run_id
ALTER TABLE observation DROP CONSTRAINT observation_fk_keyvaluemapping_id
ALTER TABLE observation DROP CONSTRAINT observation_fk_project_id


-- project foreign keys

ALTER TABLE project DROP CONSTRAINT project_fk_code_version_snapshot_id


-- tag_mapping foreign keys

ALTER TABLE tag_mapping DROP CONSTRAINT fk_project_id
ALTER TABLE tag_mapping DROP CONSTRAINT tagmapping_fk_dataset_id
ALTER TABLE tag_mapping DROP CONSTRAINT tagmapping_fk_dataset_version_id
ALTER TABLE tag_mapping DROP CONSTRAINT tagmapping_fk_experiment_id
ALTER TABLE tag_mapping DROP CONSTRAINT tagmapping_fk_experiment_run_id

-- foreign keys

ALTER TABLE versioning_modeldb_entity_mapping DROP CONSTRAINT ck_versioning_modeldb_entity_mapping
ALTER TABLE branch DROP CONSTRAINT fk_commit_hash_branchs
ALTER TABLE commit_parent DROP CONSTRAINT fk_child_hash_commit_parent
ALTER TABLE commit_parent DROP CONSTRAINT fk_parent_hash_commit_parent
ALTER TABLE repository_commit DROP CONSTRAINT fk_entity_id_repository_entity_mapping
ALTER TABLE tag DROP CONSTRAINT fk_commit_hash_tags
ALTER TABLE environment_blob DROP CONSTRAINT fk_docker_environment_blob
ALTER TABLE notebook_code_blob DROP CONSTRAINT fk_git_code_blob
ALTER TABLE git_snapshot_file_paths DROP CONSTRAINT fk_gitsnapshotentity_id

ALTER TABLE hyperparameter_set_config_blob DROP CONSTRAINT fk_begin_hyperparameter_element_config_blob
ALTER TABLE hyperparameter_set_config_blob DROP CONSTRAINT fk_end_hyperparameter_element_config_blob
ALTER TABLE hyperparameter_set_config_blob DROP CONSTRAINT fk_step_hyperparameter_element_config_blob
ALTER TABLE config_blob DROP CONSTRAINT fk_cb_hyperparameter_element_config_blob
ALTER TABLE hyperparameter_discrete_set_element_mapping DROP CONSTRAINT fk_hyperparameter_element_config_blob
ALTER TABLE dataset_part_info DROP CONSTRAINT [createTable-9_fk_path_dataset_version_info_id]
ALTER TABLE dataset_version DROP CONSTRAINT datasetversion_fk_path_dataset_version_info_id
ALTER TABLE environment_blob DROP CONSTRAINT fk_python_environment_blob
ALTER TABLE python_environment_requirements_blob DROP CONSTRAINT fk_perb_python_environment_blob
ALTER TABLE query_parameter DROP CONSTRAINT query_parameter_fk_query_dataset_version_info_id
ALTER TABLE dataset_version DROP CONSTRAINT datasetversion_fk_query_dataset_version_info_id
ALTER TABLE dataset_version DROP CONSTRAINT datasetversion_fk_raw_dataset_version_info_id
ALTER TABLE branch DROP CONSTRAINT fk_repository_id_branchs
ALTER TABLE dataset_repository_mapping DROP CONSTRAINT fk_repository_id
ALTER TABLE repository_commit DROP CONSTRAINT fk_repository_id_commit_hash_mapping
ALTER TABLE tag DROP CONSTRAINT fk_repository_id_tags
ALTER TABLE environment_command_line DROP CONSTRAINT fk_environment_blob_hash
ALTER TABLE environment_variables DROP CONSTRAINT fk_environment_blob
ALTER TABLE config_blob DROP CONSTRAINT fk_cb_hyperparameter_set_config_blob
ALTER TABLE hyperparameter_discrete_set_element_mapping DROP CONSTRAINT fk_hyperparameter_set_config_blob

-- artifact_part definition
-- Drop table
DROP TABLE artifact_part;

-- artifact_store definition
-- Drop table
DROP TABLE artifact_store;

-- [attribute] definition
-- Drop table
DROP TABLE [attribute];

-- audit_resource_workspace_mapping definition
-- Drop table
DROP TABLE audit_resource_workspace_mapping;

-- audit_service_local_audit_log definition
-- Drop table
DROP TABLE audit_service_local_audit_log;

-- user_comment definition
-- Drop table
DROP TABLE user_comment;

-- comment definition
-- Drop table
DROP TABLE comment;

-- versioning_modeldb_entity_mapping_config_blob definition
-- Drop table
DROP TABLE versioning_modeldb_entity_mapping_config_blob;

-- versioning_modeldb_entity_mapping definition
-- Drop table
DROP TABLE versioning_modeldb_entity_mapping;

-- branch definition
-- Drop table
DROP TABLE branch;

-- commit_parent definition
-- Drop table
DROP TABLE commit_parent;

-- repository_commit definition
-- Drop table
DROP TABLE repository_commit;

-- [commit] definition
-- Drop table
DROP TABLE [commit];

-- database_change_log definition
-- Drop table
DROP TABLE database_change_log;

-- database_change_log_lock definition
-- Drop table
DROP TABLE database_change_log_lock;

-- dataset definition
-- Drop table
DROP TABLE dataset;

-- docker_environment_blob definition
-- Drop table
DROP TABLE docker_environment_blob;

-- event definition
-- Drop table
DROP TABLE event;

-- folder_element definition
-- Drop table
DROP TABLE folder_element;

-- git_code_blob definition
-- Drop table
DROP TABLE git_code_blob;

-- git_snapshot definition
-- Drop table
DROP TABLE git_snapshot;

-- hyperparameter_element_config_blob definition
-- Drop table
DROP TABLE hyperparameter_element_config_blob;

-- job definition
-- Drop table
DROP TABLE job;

-- key_value_property_mapping definition
-- Drop table
DROP TABLE key_value_property_mapping;

-- labels_mapping definition
-- Drop table
DROP TABLE labels_mapping;

-- lineage definition
-- Drop table
DROP TABLE lineage;

-- metadata_property_mapping definition
-- Drop table
DROP TABLE metadata_property_mapping;

-- migration_status definition
-- Drop table
DROP TABLE migration_status;

-- modeldb_deployment_info definition
-- Drop table
DROP TABLE modeldb_deployment_info;

-- path_dataset_component_blob definition
-- Drop table
DROP TABLE path_dataset_component_blob;

-- path_dataset_version_info definition
-- Drop table
DROP TABLE path_dataset_version_info;

-- python_environment_blob definition
-- Drop table
DROP TABLE python_environment_blob;

-- query_dataset_component_blob definition
-- Drop table
DROP TABLE query_dataset_component_blob;

-- query_dataset_version_info definition
-- Drop table
DROP TABLE query_dataset_version_info;

-- raw_dataset_version_info definition
-- Drop table
DROP TABLE raw_dataset_version_info;

-- repository definition
-- Drop table
DROP TABLE repository;

-- s3_dataset_component_blob definition
-- Drop table
DROP TABLE s3_dataset_component_blob;

-- telemetry_information definition
-- Drop table
DROP TABLE telemetry_information;

-- upload_status definition
-- Drop table
DROP TABLE upload_status;

-- dataset_part_info definition
-- Drop table
DROP TABLE dataset_part_info;

-- dataset_repository_mapping definition
-- Drop table
DROP TABLE dataset_repository_mapping;

-- dataset_version definition
-- Drop table
DROP TABLE dataset_version;

-- environment_blob definition
-- Drop table
DROP TABLE environment_blob;

-- environment_command_line definition
-- Drop table
DROP TABLE environment_command_line;

-- environment_variables definition
-- Drop table
DROP TABLE environment_variables;

-- git_snapshot_file_paths definition
-- Drop table
DROP TABLE git_snapshot_file_paths;

-- hyperparameter_set_config_blob definition
-- Drop table
DROP TABLE hyperparameter_set_config_blob;

-- notebook_code_blob definition
-- Drop table
DROP TABLE notebook_code_blob;

-- python_environment_requirements_blob definition
-- Drop table
DROP TABLE python_environment_requirements_blob;

-- query_parameter definition
-- Drop table
DROP TABLE query_parameter;

-- tag definition
-- Drop table
DROP TABLE tag;

-- config_blob definition
-- Drop table
DROP TABLE config_blob;

-- hyperparameter_discrete_set_element_mapping definition
-- Drop table
DROP TABLE hyperparameter_discrete_set_element_mapping;

-- artifact definition
-- Drop table
DROP TABLE artifact;

-- code_version definition
-- Drop table
DROP TABLE code_version;

-- experiment definition
-- Drop table
DROP TABLE experiment;

-- experiment_run definition
-- Drop table
DROP TABLE experiment_run;

-- feature definition
-- Drop table
DROP TABLE feature;

-- hyperparameter_element_mapping definition
-- Drop table
DROP TABLE hyperparameter_element_mapping;

-- keyvalue definition
-- Drop table
DROP TABLE keyvalue;

-- observation definition
-- Drop table
DROP TABLE observation;

-- project definition
-- Drop table
DROP TABLE project;

-- tag_mapping definition
-- Drop table
DROP TABLE tag_mapping;

COMMIT TRANSACTION