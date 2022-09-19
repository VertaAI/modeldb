package ai.verta.modeldb;

import ai.verta.modeldb.comment.CommentDAO;
import ai.verta.modeldb.comment.CommentDAORdbImpl;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAODisabled;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAORdbImpl;
import ai.verta.modeldb.common.artifactStore.storageservice.NoopArtifactStoreService;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.configuration.ReconcilerInitializer;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.dataset.DatasetDAORdbImpl;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAORdbImpl;
import ai.verta.modeldb.experiment.FutureExperimentDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAORdbImpl;
import ai.verta.modeldb.experimentRun.FutureExperimentRunDAO;
import ai.verta.modeldb.lineage.LineageDAO;
import ai.verta.modeldb.lineage.LineageDAORdbImpl;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataDAORdbImpl;
import ai.verta.modeldb.project.FutureProjectDAO;
import ai.verta.modeldb.utils.UACApisUtil;
import ai.verta.modeldb.versioning.*;
import ai.verta.uac.ServiceEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.concurrent.Executor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter(AccessLevel.PRIVATE)
public class DAOSet {
  @JsonProperty private ArtifactStoreDAO artifactStoreDAO;
  @JsonProperty private BlobDAO blobDAO;
  @JsonProperty private CommentDAO commentDAO;
  @JsonProperty private CommitDAO commitDAO;
  @JsonProperty private DatasetDAO datasetDAO;
  @JsonProperty private DatasetVersionDAO datasetVersionDAO;
  @JsonProperty private FutureExperimentDAO futureExperimentDAO;
  @JsonProperty private ExperimentRunDAO experimentRunDAO;
  @JsonProperty private FutureExperimentRunDAO futureExperimentRunDAO;
  @JsonProperty private FutureProjectDAO futureProjectDAO;
  @JsonProperty private LineageDAO lineageDAO;
  @JsonProperty private MetadataDAO metadataDAO;
  @JsonProperty private RepositoryDAO repositoryDAO;
  @JsonProperty private FutureEventDAO futureEventDAO;
  @JsonProperty private UACApisUtil uacApisUtil;

  public static DAOSet fromServices(
      ServiceSet services,
      FutureJdbi jdbi,
      Executor executor,
      MDBConfig mdbConfig,
      ReconcilerInitializer reconcilerInitializer) {
    var set = new DAOSet();
    set.uacApisUtil = new UACApisUtil(executor, services.getUac());

    set.metadataDAO = new MetadataDAORdbImpl();
    set.commitDAO = new CommitDAORdbImpl(services.getAuthService(), services.getMdbRoleService());
    set.repositoryDAO =
        new RepositoryDAORdbImpl(
            services.getAuthService(),
            services.getMdbRoleService(),
            set.commitDAO,
            set.metadataDAO);
    set.blobDAO = new BlobDAORdbImpl(services.getAuthService(), services.getMdbRoleService());

    set.experimentRunDAO =
        new ExperimentRunDAORdbImpl(
            mdbConfig,
            services.getAuthService(),
            services.getMdbRoleService(),
            set.repositoryDAO,
            set.commitDAO,
            set.blobDAO,
            set.metadataDAO);
    if (services.getArtifactStoreService() == null
        || services.getArtifactStoreService() instanceof NoopArtifactStoreService) {
      set.artifactStoreDAO = new ArtifactStoreDAODisabled();
    } else {
      set.artifactStoreDAO =
          new ArtifactStoreDAORdbImpl(
              services.getArtifactStoreService(), mdbConfig.getArtifactStoreConfig());
    }

    set.commentDAO = new CommentDAORdbImpl(services.getAuthService());
    set.datasetDAO = new DatasetDAORdbImpl(services.getAuthService(), services.getMdbRoleService());
    set.lineageDAO = new LineageDAORdbImpl();
    set.datasetVersionDAO =
        new DatasetVersionDAORdbImpl(services.getAuthService(), services.getMdbRoleService());
    set.futureExperimentRunDAO =
        new FutureExperimentRunDAO(
            executor,
            jdbi,
            mdbConfig,
            services.getUac(),
            set.artifactStoreDAO,
            set.datasetVersionDAO,
            set.repositoryDAO,
            set.commitDAO,
            set.blobDAO,
            set.uacApisUtil);
    set.futureProjectDAO =
        new FutureProjectDAO(
            executor,
            jdbi,
            services.getUac(),
            set.artifactStoreDAO,
            set.datasetVersionDAO,
            mdbConfig,
            set.futureExperimentRunDAO,
            set.uacApisUtil,
            reconcilerInitializer);
    set.futureEventDAO =
        new FutureEventDAO(executor, jdbi, mdbConfig, ServiceEnum.Service.MODELDB_SERVICE.name());
    set.futureExperimentDAO =
        new FutureExperimentDAO(executor, jdbi, services.getUac(), mdbConfig, set);

    return set;
  }
}
