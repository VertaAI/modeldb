-- MySQL dump 10.13  Distrib 8.0.12, for Win64 (x86_64)
--
-- Host: localhost    Database: ddl_release_2022_08
-- ------------------------------------------------------
-- Server version	8.0.12

BEGIN /*!90000 PESSIMISTIC */;

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

DROP TABLE IF EXISTS `artifact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `artifact_part`
--

DROP TABLE IF EXISTS `artifact_part`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `artifact_store`
--

DROP TABLE IF EXISTS `artifact_store`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `attribute`
--

DROP TABLE IF EXISTS `attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `audit_resource_workspace_mapping`
--

DROP TABLE IF EXISTS `audit_resource_workspace_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `audit_service_local_audit_log`
--

DROP TABLE IF EXISTS `audit_service_local_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `branch`
--

DROP TABLE IF EXISTS `branch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `code_version`
--

DROP TABLE IF EXISTS `code_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `comment`
--

DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `commit`
--

DROP TABLE IF EXISTS `commit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `commit_parent`
--

DROP TABLE IF EXISTS `commit_parent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `config_blob`
--

DROP TABLE IF EXISTS `config_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `database_change_log`
--

DROP TABLE IF EXISTS `database_change_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `database_change_log_lock`
--

DROP TABLE IF EXISTS `database_change_log_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `dataset`
--

DROP TABLE IF EXISTS `dataset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `dataset_migration_status`
--

DROP TABLE IF EXISTS `dataset_migration_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `dataset_part_info`
--

DROP TABLE IF EXISTS `dataset_part_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `dataset_repository_mapping`
--

DROP TABLE IF EXISTS `dataset_repository_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `dataset_version`
--

DROP TABLE IF EXISTS `dataset_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `docker_environment_blob`
--

DROP TABLE IF EXISTS `docker_environment_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `environment_blob`
--

DROP TABLE IF EXISTS `environment_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `environment_command_line`
--

DROP TABLE IF EXISTS `environment_command_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `environment_variables`
--

DROP TABLE IF EXISTS `environment_variables`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `event`
--

DROP TABLE IF EXISTS `event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `experiment`
--

DROP TABLE IF EXISTS `experiment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `experiment_run`
--

DROP TABLE IF EXISTS `experiment_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `feature`
--

DROP TABLE IF EXISTS `feature`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `folder_element`
--

DROP TABLE IF EXISTS `folder_element`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `git_code_blob`
--

DROP TABLE IF EXISTS `git_code_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `git_snapshot`
--

DROP TABLE IF EXISTS `git_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `git_snapshot_file_paths`
--

DROP TABLE IF EXISTS `git_snapshot_file_paths`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `hyperparameter_discrete_set_element_mapping`
--

DROP TABLE IF EXISTS `hyperparameter_discrete_set_element_mapping`;

--
-- Table structure for table `hyperparameter_element_config_blob`
--

DROP TABLE IF EXISTS `hyperparameter_element_config_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `hyperparameter_element_mapping`
--

DROP TABLE IF EXISTS `hyperparameter_element_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `hyperparameter_set_config_blob`
--

DROP TABLE IF EXISTS `hyperparameter_set_config_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `job`
--

DROP TABLE IF EXISTS `job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `key_value_property_mapping`
--

DROP TABLE IF EXISTS `key_value_property_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `keyvalue`
--

DROP TABLE IF EXISTS `keyvalue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `labels_mapping`
--

DROP TABLE IF EXISTS `labels_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `lineage`
--

DROP TABLE IF EXISTS `lineage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `metadata_property_mapping`
--

DROP TABLE IF EXISTS `metadata_property_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `migration_status`
--

DROP TABLE IF EXISTS `migration_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `modeldb_deployment_info`
--

DROP TABLE IF EXISTS `modeldb_deployment_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `notebook_code_blob`
--

DROP TABLE IF EXISTS `notebook_code_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `observation`
--

DROP TABLE IF EXISTS `observation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `path_dataset_component_blob`
--

DROP TABLE IF EXISTS `path_dataset_component_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `path_dataset_version_info`
--

DROP TABLE IF EXISTS `path_dataset_version_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `python_environment_blob`
--

DROP TABLE IF EXISTS `python_environment_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `python_environment_requirements_blob`
--

DROP TABLE IF EXISTS `python_environment_requirements_blob`;

--
-- Table structure for table `query_dataset_component_blob`
--

DROP TABLE IF EXISTS `query_dataset_component_blob`;

--
-- Table structure for table `query_dataset_version_info`
--

DROP TABLE IF EXISTS `query_dataset_version_info`;

--
-- Table structure for table `query_parameter`
--

DROP TABLE IF EXISTS `query_parameter`;

--
-- Table structure for table `raw_dataset_version_info`
--

DROP TABLE IF EXISTS `raw_dataset_version_info`;

--
-- Table structure for table `repository`
--

DROP TABLE IF EXISTS `repository`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `repository_commit`
--

DROP TABLE IF EXISTS `repository_commit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `s3_dataset_component_blob`
--

DROP TABLE IF EXISTS `s3_dataset_component_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `tag`
--

DROP TABLE IF EXISTS `tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `tag_mapping`
--

DROP TABLE IF EXISTS `tag_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `telemetry_information`
--

DROP TABLE IF EXISTS `telemetry_information`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `upload_status`
--

DROP TABLE IF EXISTS `upload_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `user_comment`
--

DROP TABLE IF EXISTS `user_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `versioning_modeldb_entity_mapping`
--

DROP TABLE IF EXISTS `versioning_modeldb_entity_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

--
-- Table structure for table `versioning_modeldb_entity_mapping_config_blob`
--

DROP TABLE IF EXISTS `versioning_modeldb_entity_mapping_config_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;

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

COMMIT;
-- Dump completed on 2022-11-08 15:14:23
