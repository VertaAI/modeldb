
-- Drop table

-- DROP TABLE artifactstore;

CREATE TABLE artifactstore (
	id int8 NOT NULL,
	client_key text NULL,
	cloud_storage_file_path text NULL,
	cloud_storage_key text NULL,
	entity_id varchar(50) NULL,
	entity_name varchar(50) NULL,
	CONSTRAINT artifactstore_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE collaborator;

CREATE TABLE collaborator (
	user_id varchar(255) NOT NULL,
	email_id varchar(255) NULL,
	share_via_type int4 NULL,
	CONSTRAINT collaborator_pkey PRIMARY KEY (user_id)
);

-- Drop table

-- DROP TABLE "comment";

CREATE TABLE "comment" (
	id varchar(255) NOT NULL,
	entity_id varchar(255) NULL,
	entity_name varchar(255) NULL,
	CONSTRAINT comment_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE dataset;

CREATE TABLE dataset (
	id varchar(255) NOT NULL,
	dataset_type int4 NULL,
	dataset_visibility int4 NULL,
	description text NULL,
	"name" varchar(255) NULL,
	"owner" varchar(255) NULL,
	time_created int8 NULL,
	time_updated int8 NULL,
	CONSTRAINT dataset_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE gitsnapshot;

CREATE TABLE gitsnapshot (
	id int8 NOT NULL,
	field_type varchar(50) NULL,
	hash varchar(255) NULL,
	is_dirty int4 NULL,
	repo varchar(255) NULL,
	CONSTRAINT gitsnapshot_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE job;

CREATE TABLE job (
	id varchar(255) NOT NULL,
	description text NULL,
	end_time varchar(255) NULL,
	job_status int4 NULL,
	job_type int4 NULL,
	"owner" varchar(255) NULL,
	start_time varchar(255) NULL,
	CONSTRAINT job_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE path_dataset_version_info;

CREATE TABLE path_dataset_version_info (
	id int8 NOT NULL,
	base_path varchar(255) NULL,
	field_type varchar(50) NULL,
	location_type int4 NULL,
	"size" int8 NULL,
	CONSTRAINT path_dataset_version_info_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE query_dataset_version_info;

CREATE TABLE query_dataset_version_info (
	id int8 NOT NULL,
	data_source_uri varchar(255) NULL,
	execution_timestamp int8 NULL,
	field_type varchar(50) NULL,
	num_records int8 NULL,
	query text NULL,
	query_template text NULL,
	CONSTRAINT query_dataset_version_info_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE raw_dataset_version_info;

CREATE TABLE raw_dataset_version_info (
	id int8 NOT NULL,
	checksum varchar(255) NULL,
	field_type varchar(50) NULL,
	num_records int8 NULL,
	object_path varchar(255) NULL,
	"size" int8 NULL,
	CONSTRAINT raw_dataset_version_info_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE collaboratormappings;

CREATE TABLE collaboratormappings (
	id int8 NOT NULL,
	collaborator_type int4 NULL,
	date_created int8 NULL,
	date_deleted int8 NULL,
	date_updated int8 NULL,
	entity_id varchar(255) NULL,
	entity_name varchar(50) NULL,
	field_type varchar(50) NULL,
	message varchar(255) NULL,
	shared_by varchar(255) NULL,
	collaborator_id varchar(255) NULL,
	CONSTRAINT collaboratormappings_pkey PRIMARY KEY (id),
	CONSTRAINT fklysef6ksenp6fssh0gi7762u7 FOREIGN KEY (collaborator_id) REFERENCES collaborator(user_id)
);

-- Drop table

-- DROP TABLE dataset_part_info;

CREATE TABLE dataset_part_info (
	id int8 NOT NULL,
	checksum varchar(255) NULL,
	entity_name varchar(50) NULL,
	field_type varchar(50) NULL,
	last_modified_at_source int8 NULL,
	"path" varchar(255) NULL,
	"size" int8 NULL,
	path_dataset_version_info_id int8 NULL,
	CONSTRAINT dataset_part_info_pkey PRIMARY KEY (id),
	CONSTRAINT fkglp4vghk1s78vudr9jnmvt7v2 FOREIGN KEY (path_dataset_version_info_id) REFERENCES path_dataset_version_info(id)
);

-- Drop table

-- DROP TABLE datasetversion;

CREATE TABLE datasetversion (
	id varchar(255) NOT NULL,
	dataset_id varchar(255) NULL,
	dataset_type int4 NULL,
	dataset_version_visibility int4 NULL,
	description text NULL,
	"owner" varchar(255) NULL,
	parent_id varchar(255) NULL,
	time_logged int8 NULL,
	time_updated int8 NULL,
	"version" int8 NULL,
	path_dataset_version_info_id int8 NULL,
	query_dataset_version_info_id int8 NULL,
	raw_dataset_version_info_id int8 NULL,
	CONSTRAINT datasetversion_pkey PRIMARY KEY (id),
	CONSTRAINT fk4axpq0odnw6n9yhom6kg0p0bw FOREIGN KEY (raw_dataset_version_info_id) REFERENCES raw_dataset_version_info(id),
	CONSTRAINT fk63372a2omdh2kx9bo7xifm13l FOREIGN KEY (query_dataset_version_info_id) REFERENCES query_dataset_version_info(id),
	CONSTRAINT fkqegkomuovtkr5iwqiqmav067 FOREIGN KEY (path_dataset_version_info_id) REFERENCES path_dataset_version_info(id)
);

-- Drop table

-- DROP TABLE gitsnapshotentity_filepaths;

CREATE TABLE gitsnapshotentity_filepaths (
	gitsnapshotentity_id int8 NOT NULL,
	filepaths varchar(255) NULL,
	CONSTRAINT fkabgd4u299tnae1u81fnxtx4wg FOREIGN KEY (gitsnapshotentity_id) REFERENCES gitsnapshot(id)
);

-- Drop table

-- DROP TABLE query_parameter;

CREATE TABLE query_parameter (
	id int8 NOT NULL,
	entity_name varchar(50) NULL,
	field_type varchar(50) NULL,
	parameter_name text NULL,
	parameter_type int4 NULL,
	parameter_value text NULL,
	query_dataset_version_info_id int8 NULL,
	CONSTRAINT query_parameter_pkey PRIMARY KEY (id),
	CONSTRAINT fk8ocpexp2jn9rqlf2loy36pwpu FOREIGN KEY (query_dataset_version_info_id) REFERENCES query_dataset_version_info(id)
);

-- Drop table

-- DROP TABLE user_comment;

CREATE TABLE user_comment (
	id varchar(255) NOT NULL,
	date_time int8 NULL,
	entity_name varchar(50) NULL,
	message varchar(255) NULL,
	user_id varchar(255) NULL,
	comment_id varchar(255) NULL,
	CONSTRAINT user_comment_pkey PRIMARY KEY (id),
	CONSTRAINT fk8run8dgvadrrwcwe5xpdscynm FOREIGN KEY (comment_id) REFERENCES comment(id)
);

-- Drop table

-- DROP TABLE artifact;

CREATE TABLE artifact (
	id int8 NOT NULL,
	artifact_type int4 NULL,
	entity_name varchar(50) NULL,
	field_type varchar(50) NULL,
	filename_extension varchar(50) NULL,
	ar_key text NULL,
	linked_artifact_id varchar(255) NULL,
	ar_path text NULL,
	path_only bool NULL,
	experiment_id varchar(255) NULL,
	experiment_run_id varchar(255) NULL,
	project_id varchar(255) NULL,
	CONSTRAINT artifact_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE codeversion;

CREATE TABLE codeversion (
	id int8 NOT NULL,
	date_logged int8 NULL,
	field_type varchar(50) NULL,
	code_archive_id int8 NULL,
	git_snapshot_id int8 NULL,
	CONSTRAINT codeversion_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE experiment;

CREATE TABLE experiment (
	id varchar(255) NOT NULL,
	date_created int8 NULL,
	date_updated int8 NULL,
	description text NULL,
	"name" varchar(255) NULL,
	"owner" varchar(255) NULL,
	project_id varchar(255) NOT NULL,
	code_version_snapshot_id int8 NULL,
	CONSTRAINT experiment_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE experimentrun;

CREATE TABLE experimentrun (
	id varchar(255) NOT NULL,
	code_version varchar(255) NULL,
	date_created int8 NULL,
	date_updated int8 NULL,
	description text NULL,
	end_time int8 NULL,
	experiment_id varchar(255) NOT NULL,
	job_id varchar(255) NULL,
	"name" varchar(255) NULL,
	"owner" varchar(255) NULL,
	parent_id varchar(255) NULL,
	project_id varchar(255) NOT NULL,
	start_time int8 NULL,
	code_version_snapshot_id int8 NULL,
	CONSTRAINT experimentrun_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE feature;

CREATE TABLE feature (
	id int8 NOT NULL,
	entity_name varchar(50) NULL,
	feature text NULL,
	experiment_id varchar(255) NULL,
	experiment_run_id varchar(255) NULL,
	project_id varchar(255) NULL,
	raw_dataset_version_info_id int8 NULL,
	CONSTRAINT feature_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE keyvalue;

CREATE TABLE keyvalue (
	id int8 NOT NULL,
	entity_name varchar(50) NULL,
	field_type varchar(50) NULL,
	kv_key text NULL,
	kv_value text NULL,
	value_type int4 NULL,
	dataset_id varchar(255) NULL,
	dataset_version_id varchar(255) NULL,
	experiment_id varchar(255) NULL,
	experiment_run_id varchar(255) NULL,
	job_id varchar(255) NULL,
	project_id varchar(255) NULL,
	CONSTRAINT keyvalue_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE observation;

CREATE TABLE observation (
	id int8 NOT NULL,
	entity_name varchar(50) NULL,
	field_type varchar(50) NULL,
	"timestamp" int8 NOT NULL,
	artifact_id int8 NULL,
	experiment_id varchar(255) NULL,
	experiment_run_id varchar(255) NULL,
	keyvaluemapping_id int8 NULL,
	project_id varchar(255) NULL,
	CONSTRAINT observation_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE project;

CREATE TABLE project (
	id varchar(255) NOT NULL,
	date_created int8 NULL,
	date_updated int8 NULL,
	description text NULL,
	"name" varchar(255) NULL,
	"owner" varchar(255) NULL,
	project_visibility int4 NULL,
	readme_text text NULL,
	short_name varchar(255) NULL,
	code_version_snapshot_id int8 NULL,
	CONSTRAINT project_pkey PRIMARY KEY (id)
);

-- Drop table

-- DROP TABLE tagmapping;

CREATE TABLE tagmapping (
	id int8 NOT NULL,
	entity_name varchar(50) NULL,
	tags text NULL,
	dataset_id varchar(255) NULL,
	dataset_version_id varchar(255) NULL,
	experiment_id varchar(255) NULL,
	experiment_run_id varchar(255) NULL,
	project_id varchar(255) NULL,
	CONSTRAINT tagmapping_pkey PRIMARY KEY (id)
);

ALTER TABLE artifact ADD CONSTRAINT fkb1510ajwbgmwqk9mk7s5madq8 FOREIGN KEY (experiment_id) REFERENCES experiment(id);
ALTER TABLE artifact ADD CONSTRAINT fkkxd7ammk6wfh5sornvyqmwa6o FOREIGN KEY (experiment_run_id) REFERENCES experimentrun(id);
ALTER TABLE artifact ADD CONSTRAINT fkqi7jpmsvst8h67mvp18lrkdol FOREIGN KEY (project_id) REFERENCES project(id);

ALTER TABLE codeversion ADD CONSTRAINT fk52kol2q55s4ih48da63mshmua FOREIGN KEY (code_archive_id) REFERENCES artifact(id);
ALTER TABLE codeversion ADD CONSTRAINT fklmregkintq62cstaboy0b8mgv FOREIGN KEY (git_snapshot_id) REFERENCES gitsnapshot(id);

ALTER TABLE experiment ADD CONSTRAINT fkabmp29pv19fw9g3rotaq4bqlk FOREIGN KEY (code_version_snapshot_id) REFERENCES codeversion(id);

ALTER TABLE experimentrun ADD CONSTRAINT fk155a2dpu2yc6kpbnf9bw9ej9e FOREIGN KEY (code_version_snapshot_id) REFERENCES codeversion(id);

ALTER TABLE feature ADD CONSTRAINT fk1809dxglqgape7roab6ppb5pj FOREIGN KEY (experiment_id) REFERENCES experiment(id);
ALTER TABLE feature ADD CONSTRAINT fk3tpwqa4p3gkvtbm5y8x7pl6bc FOREIGN KEY (project_id) REFERENCES project(id);
ALTER TABLE feature ADD CONSTRAINT fk7npjb7lsh1lnc5sjfs5go1q9k FOREIGN KEY (experiment_run_id) REFERENCES experimentrun(id);
ALTER TABLE feature ADD CONSTRAINT fkiixqgx81l0gco5buvig6cqe2e FOREIGN KEY (raw_dataset_version_info_id) REFERENCES raw_dataset_version_info(id);

ALTER TABLE keyvalue ADD CONSTRAINT fk1j6qxx2f4tu0vhk6ncoigcer1 FOREIGN KEY (job_id) REFERENCES job(id);
ALTER TABLE keyvalue ADD CONSTRAINT fk4kpjcj5k42wy7jixlaw69t39e FOREIGN KEY (project_id) REFERENCES project(id);
ALTER TABLE keyvalue ADD CONSTRAINT fkaotbvffphusp2xibg54y1807c FOREIGN KEY (dataset_version_id) REFERENCES datasetversion(id);
ALTER TABLE keyvalue ADD CONSTRAINT fkdd3gul9rqi1nwo1ue8dnjkloi FOREIGN KEY (experiment_run_id) REFERENCES experimentrun(id);
ALTER TABLE keyvalue ADD CONSTRAINT fke5e22km3s8b4v6fmd5agi1169 FOREIGN KEY (dataset_id) REFERENCES dataset(id);
ALTER TABLE keyvalue ADD CONSTRAINT fks8b954cb584lfhxvpfqufp00c FOREIGN KEY (experiment_id) REFERENCES experiment(id);

ALTER TABLE observation ADD CONSTRAINT fk3l830ric355y2m15auy29nugd FOREIGN KEY (keyvaluemapping_id) REFERENCES keyvalue(id);
ALTER TABLE observation ADD CONSTRAINT fklphf7x2yos9jxj1dafev0woqp FOREIGN KEY (experiment_id) REFERENCES experiment(id);
ALTER TABLE observation ADD CONSTRAINT fkqukq600uk7ow0smk86lbecm16 FOREIGN KEY (artifact_id) REFERENCES artifact(id);
ALTER TABLE observation ADD CONSTRAINT fkrtaesogbnqbnfcg4r9oug3b5p FOREIGN KEY (experiment_run_id) REFERENCES experimentrun(id);
ALTER TABLE observation ADD CONSTRAINT fktng8v4k62d498u95g3cly1dcp FOREIGN KEY (project_id) REFERENCES project(id);

ALTER TABLE project ADD CONSTRAINT fk7h4kclvrdgs3rp838yoj0utc4 FOREIGN KEY (code_version_snapshot_id) REFERENCES codeversion(id);

ALTER TABLE tagmapping ADD CONSTRAINT fk6don0cq34v6j75a6ddvfft5mq FOREIGN KEY (experiment_id) REFERENCES experiment(id);
ALTER TABLE tagmapping ADD CONSTRAINT fk7qfxoshi1y5vnn42yvxklr7hm FOREIGN KEY (experiment_run_id) REFERENCES experimentrun(id);
ALTER TABLE tagmapping ADD CONSTRAINT fkcsd9g3kgclunkjsigfn15pnl2 FOREIGN KEY (dataset_version_id) REFERENCES datasetversion(id);
ALTER TABLE tagmapping ADD CONSTRAINT fkht136fxcojwibhx48k7iolc1u FOREIGN KEY (project_id) REFERENCES project(id);
ALTER TABLE tagmapping ADD CONSTRAINT fko3adl5c9mq66v4btyl9wwmbh FOREIGN KEY (dataset_id) REFERENCES dataset(id);
