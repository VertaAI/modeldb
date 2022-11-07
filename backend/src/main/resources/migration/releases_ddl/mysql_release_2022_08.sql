-- MySQL dump 10.13  Distrib 8.0.12, for Win64 (x86_64)
--
-- Host: localhost    Database: release_2022_08
-- ------------------------------------------------------
-- Server version	8.0.12

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
 SET NAMES utf8mb4 ;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `artifact`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `artifact` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `artifact_type` int(11) DEFAULT NULL,
  `entity_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `filename_extension` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ar_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `linked_artifact_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ar_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `path_only` bit(1) DEFAULT NULL,
  `experiment_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `experiment_run_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `project_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `store_type_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `upload_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `upload_completed` bit(1) DEFAULT b'1',
  `serialization` text COLLATE utf8mb4_general_ci,
  `artifact_subtype` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `backup_linked_artifact_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `a_e_id` (`experiment_id`),
  KEY `a_er_id` (`experiment_run_id`),
  KEY `a_p_id` (`project_id`),
  KEY `a_l_a_id` (`linked_artifact_id`),
  CONSTRAINT `artifact_fk_experiment_id` FOREIGN KEY (`experiment_id`) REFERENCES `experiment` (`id`),
  CONSTRAINT `artifact_fk_experiment_run_id` FOREIGN KEY (`experiment_run_id`) REFERENCES `experiment_run` (`id`),
  CONSTRAINT `artifact_fk_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `artifact_part`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `artifact_part` (
  `artifact_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `part_number` bigint(20) NOT NULL,
  `etag` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `artifact_type` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`artifact_id`,`part_number`,`artifact_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `artifact_store`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `artifact_store` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `client_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `cloud_storage_file_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `cloud_storage_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `entity_id` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `entity_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attribute`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `attribute` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entity_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `kv_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `kv_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `value_type` int(11) DEFAULT NULL,
  `dataset_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `dataset_version_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `experiment_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `experiment_run_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `job_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `project_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `repository_id` bigint(20) DEFAULT NULL,
  `entity_hash` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `at_d_id` (`dataset_id`),
  KEY `at_dsv_id` (`dataset_version_id`),
  KEY `at_e_id` (`experiment_id`),
  KEY `at_er_id` (`experiment_run_id`),
  KEY `at_j_id` (`job_id`),
  KEY `at_p_id` (`project_id`),
  KEY `attr_id` (`id`),
  KEY `attr_field_type` (`field_type`),
  KEY `index_entity_hash` (`entity_hash`),
  KEY `at_kv_key` (`kv_key`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_resource_workspace_mapping`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `audit_resource_workspace_mapping` (
  `audit_log_id` bigint(20) DEFAULT NULL,
  `resource_id` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `workspace_id` bigint(20) DEFAULT NULL,
  KEY `audit_fk_audit_service_local_audit_log_id` (`audit_log_id`),
  CONSTRAINT `audit_fk_audit_service_local_audit_log_id` FOREIGN KEY (`audit_log_id`) REFERENCES `audit_service_local_audit_log` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_service_local_audit_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `audit_service_local_audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `local_id` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `action` int(11) NOT NULL,
  `resource_type` int(11) DEFAULT NULL,
  `resource_service` int(11) DEFAULT NULL,
  `ts_nano` bigint(20) DEFAULT NULL,
  `method_name` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `request` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `response` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `workspace_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `local_id` (`local_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `branch`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `branch` (
  `branch` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `commit_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `repository_id` bigint(20) NOT NULL,
  PRIMARY KEY (`branch`,`repository_id`),
  KEY `fk_commit_hash_branchs` (`commit_hash`),
  KEY `fk_repository_id_branchs` (`repository_id`),
  CONSTRAINT `fk_commit_hash_branchs` FOREIGN KEY (`commit_hash`) REFERENCES `commit` (`commit_hash`),
  CONSTRAINT `fk_repository_id_branchs` FOREIGN KEY (`repository_id`) REFERENCES `repository` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `code_version`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `code_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `date_logged` bigint(20) DEFAULT NULL,
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `code_archive_id` bigint(20) DEFAULT NULL,
  `git_snapshot_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `cv_gss_id` (`git_snapshot_id`),
  KEY `cv_ca_id` (`code_archive_id`),
  CONSTRAINT `fk_code_archive_id` FOREIGN KEY (`code_archive_id`) REFERENCES `artifact` (`id`),
  CONSTRAINT `fk_git_snapshot_id` FOREIGN KEY (`git_snapshot_id`) REFERENCES `git_snapshot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `comment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `comment` (
  `id` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `entity_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `entity_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `c_e_id` (`entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `commit`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `commit` (
  `commit_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `date_created` bigint(20) DEFAULT NULL,
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `root_sha` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `date_updated` bigint(20) DEFAULT NULL,
  `version_number` bigint(20) DEFAULT '1',
  PRIMARY KEY (`commit_hash`),
  KEY `index_commit_hash_date_created` (`commit_hash`,`date_created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `commit_parent`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `commit_parent` (
  `parent_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `child_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `parent_order` int(11) DEFAULT NULL,
  KEY `fk_child_hash_commit_parent` (`child_hash`),
  KEY `fk_parent_hash_commit_parent` (`parent_hash`),
  CONSTRAINT `fk_child_hash_commit_parent` FOREIGN KEY (`child_hash`) REFERENCES `commit` (`commit_hash`),
  CONSTRAINT `fk_parent_hash_commit_parent` FOREIGN KEY (`parent_hash`) REFERENCES `commit` (`commit_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `config_blob` (
  `blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `config_seq_number` int(11) NOT NULL,
  `hyperparameter_type` int(11) DEFAULT NULL,
  `hyperparameter_set_config_blob_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `hyperparameter_element_config_blob_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`blob_hash`,`config_seq_number`),
  KEY `fk_cb_hyperparameter_set_config_blob` (`hyperparameter_set_config_blob_hash`),
  KEY `fk_cb_hyperparameter_element_config_blob` (`hyperparameter_element_config_blob_hash`),
  CONSTRAINT `fk_cb_hyperparameter_element_config_blob` FOREIGN KEY (`hyperparameter_element_config_blob_hash`) REFERENCES `hyperparameter_element_config_blob` (`blob_hash`),
  CONSTRAINT `fk_cb_hyperparameter_set_config_blob` FOREIGN KEY (`hyperparameter_set_config_blob_hash`) REFERENCES `hyperparameter_set_config_blob` (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `database_change_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `database_change_log` (
  `ID` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `AUTHOR` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `FILENAME` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `DATEEXECUTED` datetime NOT NULL,
  `ORDEREXECUTED` int(11) NOT NULL,
  `EXECTYPE` varchar(10) COLLATE utf8mb4_general_ci NOT NULL,
  `MD5SUM` varchar(35) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DESCRIPTION` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `COMMENTS` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TAG` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `LIQUIBASE` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CONTEXTS` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `LABELS` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `database_change_log_lock`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `database_change_log_lock` (
  `ID` int(11) NOT NULL,
  `LOCKED` bit(1) NOT NULL,
  `LOCKGRANTED` datetime DEFAULT NULL,
  `LOCKEDBY` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `dataset` (
  `id` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `dataset_type` int(11) DEFAULT NULL,
  `dataset_visibility` int(11) DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `time_created` bigint(20) DEFAULT NULL,
  `time_updated` bigint(20) DEFAULT NULL,
  `workspace` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `workspace_type` int(11) DEFAULT NULL,
  `deleted` bit(1) DEFAULT b'0',
  `workspace_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_dataset_id_time_updated` (`id`,`time_updated`),
  KEY `index_dataset_id_deleted` (`id`,`deleted`),
  KEY `d_name_wspace_wspace_type_deleted` (`name`,`workspace`,`workspace_type`,`deleted`),
  KEY `index_dataset_deleted` (`deleted`),
  KEY `index_dataset_time_updated` (`time_updated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_migration_status`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `dataset_migration_status` (
  `dataset_id` varchar(225) COLLATE utf8mb4_general_ci NOT NULL,
  `repo_id` bigint(20) NOT NULL,
  `status` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_part_info`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `dataset_part_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `checksum` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `entity_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `last_modified_at_source` bigint(20) DEFAULT NULL,
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  `path_dataset_version_info_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `dsp_pdsv_id` (`path_dataset_version_info_id`),
  CONSTRAINT `createTable-9_fk_path_dataset_version_info_id` FOREIGN KEY (`path_dataset_version_info_id`) REFERENCES `path_dataset_version_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_repository_mapping`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `dataset_repository_mapping` (
  `repository_id` bigint(20) NOT NULL,
  PRIMARY KEY (`repository_id`),
  CONSTRAINT `fk_repository_id` FOREIGN KEY (`repository_id`) REFERENCES `repository` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_version`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `dataset_version` (
  `id` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `dataset_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `dataset_type` int(11) DEFAULT NULL,
  `dataset_version_visibility` int(11) DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `parent_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `time_logged` bigint(20) DEFAULT NULL,
  `time_updated` bigint(20) DEFAULT NULL,
  `version` bigint(20) DEFAULT NULL,
  `path_dataset_version_info_id` bigint(20) DEFAULT NULL,
  `query_dataset_version_info_id` bigint(20) DEFAULT NULL,
  `raw_dataset_version_info_id` bigint(20) DEFAULT NULL,
  `deleted` bit(1) DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `dsv_qdsv_id` (`query_dataset_version_info_id`),
  KEY `dsv_rdsv_id` (`raw_dataset_version_info_id`),
  KEY `dsv_pdsv_id` (`path_dataset_version_info_id`),
  KEY `index_dv_dataset_id_time_updated` (`dataset_id`,`time_updated`),
  KEY `index_dv_dataset_id` (`dataset_id`),
  KEY `index_dataset_version_id_deleted` (`id`,`deleted`),
  KEY `index_dataset_version_dataset_id_version_deleted` (`dataset_id`,`version`,`deleted`),
  KEY `index_dataset_version_deleted` (`deleted`),
  CONSTRAINT `datasetversion_fk_path_dataset_version_info_id` FOREIGN KEY (`path_dataset_version_info_id`) REFERENCES `path_dataset_version_info` (`id`),
  CONSTRAINT `datasetversion_fk_query_dataset_version_info_id` FOREIGN KEY (`query_dataset_version_info_id`) REFERENCES `query_dataset_version_info` (`id`),
  CONSTRAINT `datasetversion_fk_raw_dataset_version_info_id` FOREIGN KEY (`raw_dataset_version_info_id`) REFERENCES `raw_dataset_version_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `docker_environment_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `docker_environment_blob` (
  `blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `repository` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tag` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sha` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `environment_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `environment_blob` (
  `blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `environment_type` int(11) DEFAULT NULL,
  `python_environment_blob_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `docker_environment_blob_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`blob_hash`),
  KEY `fk_python_environment_blob` (`python_environment_blob_hash`),
  KEY `fk_docker_environment_blob` (`docker_environment_blob_hash`),
  CONSTRAINT `fk_docker_environment_blob` FOREIGN KEY (`docker_environment_blob_hash`) REFERENCES `docker_environment_blob` (`blob_hash`),
  CONSTRAINT `fk_python_environment_blob` FOREIGN KEY (`python_environment_blob_hash`) REFERENCES `python_environment_blob` (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `environment_command_line`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `environment_command_line` (
  `environment_blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `command_seq_number` int(11) NOT NULL,
  `command` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`environment_blob_hash`,`command_seq_number`),
  CONSTRAINT `fk_environment_blob_hash` FOREIGN KEY (`environment_blob_hash`) REFERENCES `environment_blob` (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `environment_variables`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `environment_variables` (
  `environment_blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `variable_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `variable_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`environment_blob_hash`,`variable_name`),
  CONSTRAINT `fk_environment_blob` FOREIGN KEY (`environment_blob_hash`) REFERENCES `environment_blob` (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `event`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `event` (
  `event_uuid` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `event_type` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `workspace_id` bigint(20) DEFAULT NULL,
  `event_metadata` text COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`event_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `experiment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `experiment` (
  `id` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `date_created` bigint(20) DEFAULT NULL,
  `date_updated` bigint(20) DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `project_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `code_version_snapshot_id` bigint(20) DEFAULT NULL,
  `deleted` bit(1) DEFAULT b'0',
  `version_number` bigint(20) DEFAULT '1',
  `created` bit(1) DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `e_cvs_id` (`code_version_snapshot_id`),
  KEY `index_exp_name_project_id` (`name`,`project_id`),
  KEY `index_exp_project_id` (`project_id`),
  KEY `index_project_id_date_updated` (`project_id`,`date_updated`),
  KEY `index_experiment_id_deleted` (`id`,`deleted`),
  KEY `index_experiment_deleted` (`deleted`),
  KEY `index_exp_date_updated` (`date_updated`),
  CONSTRAINT `experiment_fk_code_version_snapshot_id` FOREIGN KEY (`code_version_snapshot_id`) REFERENCES `code_version` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `experiment_run`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `experiment_run` (
  `id` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `code_version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `date_created` bigint(20) DEFAULT NULL,
  `date_updated` bigint(20) DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `end_time` bigint(20) DEFAULT NULL,
  `experiment_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `job_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `parent_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `project_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `start_time` bigint(20) DEFAULT NULL,
  `code_version_snapshot_id` bigint(20) DEFAULT NULL,
  `deleted` bit(1) DEFAULT b'0',
  `environment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `created` bit(1) DEFAULT b'0',
  `version_number` bigint(20) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `er_cvs_id` (`code_version_snapshot_id`),
  KEY `er_dc` (`date_created`),
  KEY `er_dp` (`date_updated`),
  KEY `er_n` (`name`),
  KEY `er_o` (`owner`),
  KEY `er_p_id` (`project_id`),
  KEY `er_experiment_id` (`experiment_id`),
  KEY `index_exp_run_name_project_id` (`name`,`project_id`),
  KEY `index_exp_run_id_project_id` (`id`,`project_id`),
  KEY `index_exp_run_project_id_experiment_id` (`project_id`,`experiment_id`),
  KEY `index_exp_run_exp_id_date_updated` (`experiment_id`,`date_updated`),
  KEY `index_experiment_run_name_pro_id_exp_id_deleted` (`name`,`project_id`,`experiment_id`,`deleted`),
  KEY `index_experiment_run_id_deleted` (`id`,`deleted`),
  KEY `index_experiment_run_deleted` (`deleted`),
  CONSTRAINT `experimentrun_fk_code_version_snapshot_id` FOREIGN KEY (`code_version_snapshot_id`) REFERENCES `code_version` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `feature`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `feature` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entity_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `feature` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `experiment_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `experiment_run_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `project_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `raw_dataset_version_info_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `f_rdsv_id` (`raw_dataset_version_info_id`),
  KEY `f_er_id` (`experiment_run_id`),
  KEY `f_p_id` (`project_id`),
  KEY `f_e_id` (`experiment_id`),
  CONSTRAINT `feature_fk_experiment_id` FOREIGN KEY (`experiment_id`) REFERENCES `experiment` (`id`),
  CONSTRAINT `feature_fk_experiment_run_id` FOREIGN KEY (`experiment_run_id`) REFERENCES `experiment_run` (`id`),
  CONSTRAINT `feature_fk_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `feature_fk_raw_dataset_version_info_id` FOREIGN KEY (`raw_dataset_version_info_id`) REFERENCES `raw_dataset_version_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `folder_element`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `folder_element` (
  `folder_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `element_sha` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `element_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `element_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`folder_hash`,`element_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `git_code_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `git_code_blob` (
  `blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `repo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `commit_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `branch` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tag` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `is_dirty` bit(1) DEFAULT NULL,
  PRIMARY KEY (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `git_snapshot`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `git_snapshot` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `hash` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `is_dirty` int(11) DEFAULT NULL,
  `repo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `git_snapshot_file_paths`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `git_snapshot_file_paths` (
  `git_snapshot_id` bigint(20) DEFAULT NULL,
  `file_paths` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `gfp_g_id` (`git_snapshot_id`),
  CONSTRAINT `fk_gitsnapshotentity_id` FOREIGN KEY (`git_snapshot_id`) REFERENCES `git_snapshot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hyperparameter_discrete_set_element_mapping`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `hyperparameter_discrete_set_element_mapping` (
  `set_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `element_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`set_hash`,`element_hash`),
  KEY `fk_hyperparameter_element_config_blob` (`element_hash`),
  CONSTRAINT `fk_hyperparameter_element_config_blob` FOREIGN KEY (`element_hash`) REFERENCES `hyperparameter_element_config_blob` (`blob_hash`),
  CONSTRAINT `fk_hyperparameter_set_config_blob` FOREIGN KEY (`set_hash`) REFERENCES `hyperparameter_set_config_blob` (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hyperparameter_element_config_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `hyperparameter_element_config_blob` (
  `blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `commit_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `value_type` int(11) DEFAULT NULL,
  `int_value` bigint(20) DEFAULT NULL,
  `float_value` double DEFAULT NULL,
  `string_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hyperparameter_element_mapping`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `hyperparameter_element_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `experiment_run_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `entity_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `int_value` bigint(20) DEFAULT NULL,
  `float_value` double DEFAULT NULL,
  `string_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `hem_fk_experiment_run_id` (`experiment_run_id`),
  CONSTRAINT `hem_fk_experiment_run_id` FOREIGN KEY (`experiment_run_id`) REFERENCES `experiment_run` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hyperparameter_set_config_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `hyperparameter_set_config_blob` (
  `blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `value_type` int(11) DEFAULT NULL,
  `interval_begin_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `interval_end_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `interval_step_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`blob_hash`),
  KEY `fk_step_hyperparameter_element_config_blob` (`interval_step_hash`),
  KEY `fk_begin_hyperparameter_element_config_blob` (`interval_begin_hash`),
  KEY `fk_end_hyperparameter_element_config_blob` (`interval_end_hash`),
  CONSTRAINT `fk_begin_hyperparameter_element_config_blob` FOREIGN KEY (`interval_begin_hash`) REFERENCES `hyperparameter_element_config_blob` (`blob_hash`),
  CONSTRAINT `fk_end_hyperparameter_element_config_blob` FOREIGN KEY (`interval_end_hash`) REFERENCES `hyperparameter_element_config_blob` (`blob_hash`),
  CONSTRAINT `fk_step_hyperparameter_element_config_blob` FOREIGN KEY (`interval_step_hash`) REFERENCES `hyperparameter_element_config_blob` (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `job`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `job` (
  `id` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `end_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `job_status` int(11) DEFAULT NULL,
  `job_type` int(11) DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `start_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `key_value_property_mapping`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `key_value_property_mapping` (
  `entity_hash` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `property_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `kv_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `kv_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`entity_hash`,`property_name`,`kv_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `keyvalue`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `keyvalue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entity_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `kv_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `kv_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `value_type` int(11) DEFAULT NULL,
  `dataset_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `dataset_version_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `experiment_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `experiment_run_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `job_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `project_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `kv_dsv_id` (`dataset_version_id`),
  KEY `kv_p_id` (`project_id`),
  KEY `kv_j_id` (`job_id`),
  KEY `kv_e_id` (`experiment_id`),
  KEY `kv_d_id` (`dataset_id`),
  KEY `kv_er_id` (`experiment_run_id`),
  KEY `kv_field_type` (`field_type`),
  KEY `kv_kv_val` (`kv_value`(255)),
  KEY `kv_kv_key` (`kv_key`(255)),
  CONSTRAINT `keyvalue_fk_dataset_id` FOREIGN KEY (`dataset_id`) REFERENCES `dataset` (`id`),
  CONSTRAINT `keyvalue_fk_dataset_version_id` FOREIGN KEY (`dataset_version_id`) REFERENCES `dataset_version` (`id`),
  CONSTRAINT `keyvalue_fk_experiment_id` FOREIGN KEY (`experiment_id`) REFERENCES `experiment` (`id`),
  CONSTRAINT `keyvalue_fk_experiment_run_id` FOREIGN KEY (`experiment_run_id`) REFERENCES `experiment_run` (`id`),
  CONSTRAINT `keyvalue_fk_job_id` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`),
  CONSTRAINT `keyvalue_fk_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `labels_mapping`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `labels_mapping` (
  `entity_hash` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `entity_type` int(11) NOT NULL,
  `label` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`entity_hash`,`entity_type`,`label`),
  KEY `index_labels_mapping_entity_hash_entity_type` (`entity_hash`,`entity_type`),
  KEY `index_labels_mapping_label` (`label`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lineage`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `lineage` (
  `input_external_id` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `input_type` int(11) NOT NULL,
  `output_external_id` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `output_type` int(11) NOT NULL,
  PRIMARY KEY (`input_external_id`,`input_type`,`output_external_id`,`output_type`),
  KEY `p_input_lineage` (`input_external_id`,`input_type`),
  KEY `p_output_lineage` (`output_external_id`,`output_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `metadata_property_mapping`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `metadata_property_mapping` (
  `repository_id` bigint(20) NOT NULL,
  `commit_sha` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `location` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `metadata_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `metadata_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`repository_id`,`commit_sha`,`location`,`metadata_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `migration_status`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `migration_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `migration_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `modeldb_deployment_info`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `modeldb_deployment_info` (
  `md_key` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `md_value` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `creation_timestamp` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notebook_code_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `notebook_code_blob` (
  `blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `git_code_blob_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `path_dataset_blob_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`blob_hash`),
  KEY `fk_git_code_blob` (`git_code_blob_hash`),
  CONSTRAINT `fk_git_code_blob` FOREIGN KEY (`git_code_blob_hash`) REFERENCES `git_code_blob` (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `observation`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `observation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entity_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `timestamp` bigint(20) NOT NULL,
  `artifact_id` bigint(20) DEFAULT NULL,
  `experiment_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `experiment_run_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `keyvaluemapping_id` bigint(20) DEFAULT NULL,
  `project_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `epoch_number` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `o_kv_id` (`keyvaluemapping_id`),
  KEY `o_e_id` (`experiment_id`),
  KEY `o_a_id` (`artifact_id`),
  KEY `o_er_id` (`experiment_run_id`),
  KEY `o_p_id` (`project_id`),
  CONSTRAINT `observation_fk_artifact_id` FOREIGN KEY (`artifact_id`) REFERENCES `artifact` (`id`),
  CONSTRAINT `observation_fk_experiment_id` FOREIGN KEY (`experiment_id`) REFERENCES `experiment` (`id`),
  CONSTRAINT `observation_fk_experiment_run_id` FOREIGN KEY (`experiment_run_id`) REFERENCES `experiment_run` (`id`),
  CONSTRAINT `observation_fk_keyvaluemapping_id` FOREIGN KEY (`keyvaluemapping_id`) REFERENCES `keyvalue` (`id`),
  CONSTRAINT `observation_fk_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `path_dataset_component_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `path_dataset_component_blob` (
  `path_dataset_blob_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `last_modified_at_source` bigint(20) DEFAULT NULL,
  `md5` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `sha256` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `size` bigint(20) DEFAULT NULL,
  `internal_versioned_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `base_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`path_dataset_blob_id`,`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `path_dataset_version_info`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `path_dataset_version_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `base_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `location_type` int(11) DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `project` (
  `id` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `date_created` bigint(20) DEFAULT NULL,
  `date_updated` bigint(20) DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `project_visibility` int(11) DEFAULT NULL,
  `readme_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `short_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `code_version_snapshot_id` bigint(20) DEFAULT NULL,
  `workspace` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `workspace_type` int(11) DEFAULT NULL,
  `deleted` bit(1) DEFAULT b'0',
  `workspace_id` bigint(20) DEFAULT NULL,
  `created` bit(1) DEFAULT b'0',
  `visibility_migration` bit(1) DEFAULT b'0',
  `version_number` bigint(20) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `p_cvs_id` (`code_version_snapshot_id`),
  KEY `p_name` (`name`),
  KEY `index_project_p_id_date_updated` (`id`,`date_updated`),
  KEY `index_project_id_deleted` (`id`,`deleted`),
  KEY `p_name_wspace_wspace_type_deleted` (`name`,`workspace`,`workspace_type`,`deleted`),
  KEY `index_project_deleted` (`deleted`),
  KEY `index_project_date_updated` (`date_updated`),
  KEY `index_visibility_migration_project` (`visibility_migration`),
  CONSTRAINT `project_fk_code_version_snapshot_id` FOREIGN KEY (`code_version_snapshot_id`) REFERENCES `code_version` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `python_environment_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `python_environment_blob` (
  `blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `major` int(11) DEFAULT NULL,
  `minor` int(11) DEFAULT NULL,
  `patch` int(11) DEFAULT NULL,
  `suffix` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `raw_requirements` text COLLATE utf8mb4_general_ci,
  `raw_constraints` text COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `python_environment_requirements_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `python_environment_requirements_blob` (
  `python_environment_blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `library` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `python_constraint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `major` int(11) DEFAULT NULL,
  `minor` int(11) DEFAULT NULL,
  `patch` int(11) DEFAULT NULL,
  `suffix` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `req_or_constraint` bit(1) NOT NULL,
  PRIMARY KEY (`python_environment_blob_hash`,`library`,`python_constraint`,`req_or_constraint`),
  CONSTRAINT `fk_perb_python_environment_blob` FOREIGN KEY (`python_environment_blob_hash`) REFERENCES `python_environment_blob` (`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `query_dataset_component_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `query_dataset_component_blob` (
  `query_dataset_blob_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `data_source_uri` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `execution_timestamp` bigint(20) DEFAULT NULL,
  `num_records` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`query_dataset_blob_id`,`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `query_dataset_version_info`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `query_dataset_version_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `data_source_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `execution_timestamp` bigint(20) DEFAULT NULL,
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `num_records` bigint(20) DEFAULT NULL,
  `query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `query_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `query_parameter`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `query_parameter` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entity_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `parameter_name` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `parameter_type` int(11) DEFAULT NULL,
  `parameter_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `query_dataset_version_info_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `qp_qdsv_id` (`query_dataset_version_info_id`),
  CONSTRAINT `query_parameter_fk_query_dataset_version_info_id` FOREIGN KEY (`query_dataset_version_info_id`) REFERENCES `query_dataset_version_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `raw_dataset_version_info`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `raw_dataset_version_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `checksum` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `num_records` bigint(20) DEFAULT NULL,
  `object_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `repository`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `repository` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `date_created` bigint(20) DEFAULT NULL,
  `date_updated` bigint(20) DEFAULT NULL,
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `workspace_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `workspace_type` int(11) DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `repository_visibility` int(11) DEFAULT NULL,
  `repository_access_modifier` int(11) DEFAULT '1',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `deleted` bit(1) DEFAULT b'0',
  `workspace_service_id` bigint(20) DEFAULT NULL,
  `created` bit(1) DEFAULT b'0',
  `visibility_migration` bit(1) DEFAULT b'0',
  `version_number` bigint(20) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `index_repo_id_date_updated` (`id`,`date_updated`),
  KEY `index_repository_id_deleted` (`id`,`deleted`),
  KEY `index_repository_deleted` (`deleted`),
  KEY `index_repo_date_updated` (`date_updated`),
  KEY `index_visibility_migration_repository` (`visibility_migration`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `repository_commit`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `repository_commit` (
  `repository_id` bigint(20) DEFAULT NULL,
  `commit_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  KEY `fk_entity_id_repository_entity_mapping` (`commit_hash`),
  KEY `index_repository_commit_repo_id` (`repository_id`),
  CONSTRAINT `fk_entity_id_repository_entity_mapping` FOREIGN KEY (`commit_hash`) REFERENCES `commit` (`commit_hash`),
  CONSTRAINT `fk_repository_id_commit_hash_mapping` FOREIGN KEY (`repository_id`) REFERENCES `repository` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `s3_dataset_component_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `s3_dataset_component_blob` (
  `s3_dataset_blob_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `blob_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `last_modified_at_source` bigint(20) DEFAULT NULL,
  `md5` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `sha256` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `size` bigint(20) DEFAULT NULL,
  `s3_version_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `internal_versioned_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `base_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`s3_dataset_blob_id`,`blob_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tag`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `tag` (
  `tag` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `commit_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `repository_id` bigint(20) NOT NULL,
  PRIMARY KEY (`tag`,`repository_id`),
  KEY `fk_commit_hash_tags` (`commit_hash`),
  KEY `fk_repository_id_tags` (`repository_id`),
  CONSTRAINT `fk_commit_hash_tags` FOREIGN KEY (`commit_hash`) REFERENCES `commit` (`commit_hash`),
  CONSTRAINT `fk_repository_id_tags` FOREIGN KEY (`repository_id`) REFERENCES `repository` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tag_mapping`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `tag_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entity_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `dataset_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `dataset_version_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `experiment_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `experiment_run_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `project_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `t_e_id` (`experiment_id`),
  KEY `t_dsv_id` (`dataset_version_id`),
  KEY `t_ds_id` (`dataset_id`),
  KEY `t_p_id` (`project_id`),
  KEY `t_er_id` (`experiment_run_id`),
  CONSTRAINT `fk_project_id` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `tagmapping_fk_dataset_id` FOREIGN KEY (`dataset_id`) REFERENCES `dataset` (`id`),
  CONSTRAINT `tagmapping_fk_dataset_version_id` FOREIGN KEY (`dataset_version_id`) REFERENCES `dataset_version` (`id`),
  CONSTRAINT `tagmapping_fk_experiment_id` FOREIGN KEY (`experiment_id`) REFERENCES `experiment` (`id`),
  CONSTRAINT `tagmapping_fk_experiment_run_id` FOREIGN KEY (`experiment_run_id`) REFERENCES `experiment_run` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `telemetry_information`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `telemetry_information` (
  `tel_key` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tel_value` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `collection_timestamp` bigint(20) DEFAULT NULL,
  `transfer_timestamp` bigint(20) DEFAULT NULL,
  `telemetry_consumer` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `upload_status`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `upload_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dataset_component_blob_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `component_blob_type` int(11) DEFAULT NULL,
  `upload_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `upload_completed` bit(1) DEFAULT b'1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_comment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `user_comment` (
  `id` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `date_time` bigint(20) DEFAULT NULL,
  `entity_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `user_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `comment_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `uc_c_id` (`comment_id`),
  CONSTRAINT `fk_comment_id` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `versioning_modeldb_entity_mapping`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `versioning_modeldb_entity_mapping` (
  `repository_id` bigint(20) DEFAULT NULL,
  `commit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `versioning_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `versioning_location` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `experiment_run_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `entity_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `versioning_blob_type` int(11) DEFAULT NULL,
  `blob_hash` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  UNIQUE KEY `ck_versioning_modeldb_entity_mapping` (`repository_id`,`commit`,`versioning_key`,`experiment_run_id`,`entity_type`),
  KEY `index_vmem_exp_run_id` (`experiment_run_id`),
  KEY `index_vmem_repo_id_commit` (`repository_id`,`commit`),
  KEY `index_vmem_versioning_location` (`versioning_location`(255)),
  KEY `index_vmem_versioning_blob_type` (`versioning_blob_type`),
  KEY `index_vmem_table` (`repository_id`,`commit`,`experiment_run_id`,`versioning_blob_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `versioning_modeldb_entity_mapping_config_blob`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `versioning_modeldb_entity_mapping_config_blob` (
  `versioning_modeldb_entity_mapping_repository_id` bigint(20) DEFAULT NULL,
  `versioning_modeldb_entity_mapping_commit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `versioning_modeldb_entity_mapping_versioning_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `versioning_modeldb_entity_mapping_experiment_run_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `versioning_modeldb_entity_mapping_entity_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config_blob_entity_blob_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config_blob_entity_config_seq_number` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'ddl_release_2022_08'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- DDL Dump completed on 2022-11-07 18:00:10
