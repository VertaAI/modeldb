package ai.verta.modeldb;

import ai.verta.modeldb.comment.CommentDAO;
import ai.verta.modeldb.comment.CommentDAORdbImpl;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAODisabled;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAORdbImpl;
import ai.verta.modeldb.common.artifactStore.storageservice.NoopArtifactStoreService;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.configuration.ReconcilerInitializer;
import ai.verta.modeldb.experiment.FutureExperimentDAO;
import ai.verta.modeldb.experimentRun.FutureExperimentRunDAO;
import ai.verta.modeldb.lineage.LineageDAO;
import ai.verta.modeldb.lineage.LineageDAORdbImpl;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataDAORdbImpl;
import ai.verta.modeldb.project.FutureProjectDAO;
import ai.verta.modeldb.utils.UACApisUtil;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.BlobDAORdbImpl;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.CommitDAORdbImpl;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryDAORdbImpl;
import ai.verta.uac.ServiceEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Setter(AccessLevel.NONE)
public class DAOSet {
  @JsonProperty private ArtifactStoreDAO artifactStoreDAO;
  @JsonProperty private BlobDAO blobDAO;
  @JsonProperty private CommentDAO commentDAO;
  @JsonProperty private CommitDAO commitDAO;
  @JsonProperty private FutureExperimentDAO futureExperimentDAO;
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
      FutureExecutor executor,
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
            set.metadataDAO,
            mdbConfig);
    set.blobDAO = new BlobDAORdbImpl(services.getAuthService(), services.getMdbRoleService());

    if (services.getArtifactStoreService() == null
        || services.getArtifactStoreService() instanceof NoopArtifactStoreService) {
      set.artifactStoreDAO = new ArtifactStoreDAODisabled();
    } else {
      set.artifactStoreDAO =
          new ArtifactStoreDAORdbImpl(
              services.getArtifactStoreService(), mdbConfig.getArtifactStoreConfig());
    }

    set.commentDAO = new CommentDAORdbImpl(services.getAuthService());
    set.lineageDAO = new LineageDAORdbImpl();
    set.futureExperimentRunDAO =
        new FutureExperimentRunDAO(
            executor,
            jdbi,
            mdbConfig,
            services.getUac(),
            set.artifactStoreDAO,
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
