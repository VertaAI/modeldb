package ai.verta.modeldb.utils;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.batchProcess.OwnerRoleBindingUtils;
import ai.verta.modeldb.batchProcess.PopulateVersionMigration;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.config.MigrationConfig;
import ai.verta.modeldb.entities.ArtifactEntity;
import ai.verta.modeldb.entities.ArtifactPartEntity;
import ai.verta.modeldb.entities.ArtifactStoreMapping;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.CodeVersionEntity;
import ai.verta.modeldb.entities.CommentEntity;
import ai.verta.modeldb.entities.DatasetEntity;
import ai.verta.modeldb.entities.DatasetPartInfoEntity;
import ai.verta.modeldb.entities.DatasetVersionEntity;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.entities.FeatureEntity;
import ai.verta.modeldb.entities.GitSnapshotEntity;
import ai.verta.modeldb.entities.JobEntity;
import ai.verta.modeldb.entities.KeyValueEntity;
import ai.verta.modeldb.entities.LineageEntity;
import ai.verta.modeldb.entities.ObservationEntity;
import ai.verta.modeldb.entities.PathDatasetVersionInfoEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.QueryDatasetVersionInfoEntity;
import ai.verta.modeldb.entities.QueryParameterEntity;
import ai.verta.modeldb.entities.RawDatasetVersionInfoEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.entities.UploadStatusEntity;
import ai.verta.modeldb.entities.UserCommentEntity;
import ai.verta.modeldb.entities.code.GitCodeBlobEntity;
import ai.verta.modeldb.entities.code.NotebookCodeBlobEntity;
import ai.verta.modeldb.entities.config.ConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterElementConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterElementMappingEntity;
import ai.verta.modeldb.entities.config.HyperparameterSetConfigBlobEntity;
import ai.verta.modeldb.entities.dataset.PathDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.dataset.QueryDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.dataset.S3DatasetComponentBlobEntity;
import ai.verta.modeldb.entities.environment.DockerEnvironmentBlobEntity;
import ai.verta.modeldb.entities.environment.EnvironmentBlobEntity;
import ai.verta.modeldb.entities.environment.EnvironmentCommandLineEntity;
import ai.verta.modeldb.entities.environment.EnvironmentVariablesEntity;
import ai.verta.modeldb.entities.environment.PythonEnvironmentBlobEntity;
import ai.verta.modeldb.entities.environment.PythonEnvironmentRequirementBlobEntity;
import ai.verta.modeldb.entities.metadata.KeyValuePropertyMappingEntity;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.metadata.MetadataPropertyMappingEntity;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.DatasetRepositoryMappingEntity;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.TagsEntity;
import ai.verta.modeldb.entities.versioning.VersioningModeldbEntityMapping;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModelDBHibernateUtil extends CommonHibernateUtil {
  private static final Logger LOGGER = LogManager.getLogger(ModelDBHibernateUtil.class);
  private static ModelDBHibernateUtil modelDBHibernateUtil;

  private ModelDBHibernateUtil() {}

  public static ModelDBHibernateUtil getInstance() {
    if (modelDBHibernateUtil == null) {
      modelDBHibernateUtil = new ModelDBHibernateUtil();
      initializedUtil();
    }
    return modelDBHibernateUtil;
  }

  private static void initializedUtil() {
    liquibaseRootFilePath = "liquibase/db-changelog-master.xml";
    entities =
        new Class[] {
          ProjectEntity.class,
          ExperimentEntity.class,
          ExperimentRunEntity.class,
          KeyValueEntity.class,
          ArtifactEntity.class,
          ArtifactPartEntity.class,
          FeatureEntity.class,
          TagsMapping.class,
          ObservationEntity.class,
          JobEntity.class,
          GitSnapshotEntity.class,
          CodeVersionEntity.class,
          DatasetEntity.class,
          DatasetVersionEntity.class,
          RawDatasetVersionInfoEntity.class,
          PathDatasetVersionInfoEntity.class,
          DatasetPartInfoEntity.class,
          QueryDatasetVersionInfoEntity.class,
          QueryParameterEntity.class,
          CommentEntity.class,
          UserCommentEntity.class,
          ArtifactStoreMapping.class,
          AttributeEntity.class,
          LineageEntity.class,
          RepositoryEntity.class,
          CommitEntity.class,
          LabelsMappingEntity.class,
          TagsEntity.class,
          PathDatasetComponentBlobEntity.class,
          S3DatasetComponentBlobEntity.class,
          InternalFolderElementEntity.class,
          EnvironmentBlobEntity.class,
          DockerEnvironmentBlobEntity.class,
          PythonEnvironmentBlobEntity.class,
          PythonEnvironmentRequirementBlobEntity.class,
          EnvironmentCommandLineEntity.class,
          EnvironmentVariablesEntity.class,
          BranchEntity.class,
          HyperparameterElementConfigBlobEntity.class,
          HyperparameterSetConfigBlobEntity.class,
          ConfigBlobEntity.class,
          GitCodeBlobEntity.class,
          NotebookCodeBlobEntity.class,
          BranchEntity.class,
          VersioningModeldbEntityMapping.class,
          HyperparameterElementMappingEntity.class,
          MetadataPropertyMappingEntity.class,
          DatasetRepositoryMappingEntity.class,
          UploadStatusEntity.class,
          KeyValuePropertyMappingEntity.class,
          QueryDatasetComponentBlobEntity.class
        };
  }

  public void initializedConfigAndDatabase(Config mdbConfig, DatabaseConfig dbConfig) {
    config = mdbConfig;
    databaseConfig = dbConfig;
  }

  /**
   * If you want to define new migration then add new if check for your migration in `if (migration)
   * {` condition.
   */
  public void runMigration(List<MigrationConfig> migrations) throws ModelDBException {
    if (migrations != null) {
      LOGGER.debug("Running code migrations.");
      for (MigrationConfig migrationConfig : migrations) {
        if (!migrationConfig.enabled) {
          continue;
        }
        switch (migrationConfig.name) {
          case ModelDBConstants.SUB_ENTITIES_OWNERS_RBAC_MIGRATION:
            OwnerRoleBindingUtils.execute();
            break;
          case ModelDBConstants.POPULATE_VERSION_MIGRATION:
            PopulateVersionMigration.execute(migrationConfig.record_update_limit);
            break;
          default:
            // Do nothing
            break;
        }
      }
    }
    LOGGER.debug("Completed code migrations.");
  }
}
