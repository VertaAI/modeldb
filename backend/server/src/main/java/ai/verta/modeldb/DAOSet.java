package ai.verta.modeldb;

import ai.verta.modeldb.comment.CommentDAO;
import ai.verta.modeldb.comment.CommentDAORdbImpl;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAODisabled;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAORdbImpl;
import ai.verta.modeldb.common.artifactStore.storageservice.NoopArtifactStoreService;
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
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.BlobDAORdbImpl;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.CommitDAORdbImpl;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryDAORdbImpl;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter(AccessLevel.NONE)
public class DAOSet {
  private ArtifactStoreDAO artifactStoreDAO;
  private BlobDAO blobDAO;
  private CommentDAO commentDAO;
  private CommitDAO commitDAO;
  private FutureExperimentDAO futureExperimentDAO;
  private FutureExperimentRunDAO futureExperimentRunDAO;
  private FutureProjectDAO futureProjectDAO;
  private LineageDAO lineageDAO;
  private MetadataDAO metadataDAO;
  private RepositoryDAO repositoryDAO;

  public static DAOSet fromServices(
      ServiceSet services,
      FutureJdbi jdbi,
      FutureExecutor executor,
      MDBConfig mdbConfig,
      ReconcilerInitializer reconcilerInitializer) {
    var set = new DAOSet();

    set.metadataDAO = new MetadataDAORdbImpl();
    set.commitDAO = new CommitDAORdbImpl(services.getUacApisUtil(), services.getMdbRoleService());
    set.repositoryDAO =
        new RepositoryDAORdbImpl(
            services.getUacApisUtil(),
            services.getMdbRoleService(),
            set.commitDAO,
            set.metadataDAO,
            mdbConfig);
    set.blobDAO = new BlobDAORdbImpl(services.getUacApisUtil(), services.getMdbRoleService());

    if (services.getArtifactStoreService() == null
        || services.getArtifactStoreService() instanceof NoopArtifactStoreService) {
      set.artifactStoreDAO = new ArtifactStoreDAODisabled();
    } else {
      set.artifactStoreDAO =
          new ArtifactStoreDAORdbImpl(
              services.getArtifactStoreService(), mdbConfig.getArtifactStoreConfig());
    }

    set.commentDAO = new CommentDAORdbImpl(services.getUacApisUtil());
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
            services.getUacApisUtil());
    set.futureProjectDAO =
        new FutureProjectDAO(
            executor,
            jdbi,
            services.getUac(),
            set.artifactStoreDAO,
            mdbConfig,
            set.futureExperimentRunDAO,
            services.getUacApisUtil(),
            reconcilerInitializer);
    set.futureExperimentDAO =
        new FutureExperimentDAO(executor, jdbi, services.getUac(), mdbConfig, set, services);

    return set;
  }
}
