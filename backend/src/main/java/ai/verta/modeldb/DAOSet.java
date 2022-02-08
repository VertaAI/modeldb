package ai.verta.modeldb;

import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAODisabled;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAORdbImpl;
import ai.verta.modeldb.comment.CommentDAO;
import ai.verta.modeldb.comment.CommentDAORdbImpl;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.dataset.DatasetDAORdbImpl;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAORdbImpl;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experiment.ExperimentDAORdbImpl;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAORdbImpl;
import ai.verta.modeldb.experimentRun.FutureExperimentRunDAO;
import ai.verta.modeldb.lineage.LineageDAO;
import ai.verta.modeldb.lineage.LineageDAORdbImpl;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataDAORdbImpl;
import ai.verta.modeldb.project.FutureProjectDAO;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.project.ProjectDAORdbImpl;
import ai.verta.modeldb.project.UACApisUtil;
import ai.verta.modeldb.versioning.*;
import ai.verta.uac.ServiceEnum;
import java.util.concurrent.Executor;

public class DAOSet {
  public ArtifactStoreDAO artifactStoreDAO;
  public BlobDAO blobDAO;
  public CommentDAO commentDAO;
  public CommitDAO commitDAO;
  public DatasetDAO datasetDAO;
  public DatasetVersionDAO datasetVersionDAO;
  public ExperimentDAO experimentDAO;
  public ExperimentRunDAO experimentRunDAO;
  public FutureExperimentRunDAO futureExperimentRunDAO;
  public FutureProjectDAO futureProjectDAO;
  public LineageDAO lineageDAO;
  public MetadataDAO metadataDAO;
  public ProjectDAO projectDAO;
  public RepositoryDAO repositoryDAO;
  public FutureEventDAO futureEventDAO;

  public static DAOSet fromServices(
      ServiceSet services, FutureJdbi jdbi, Executor executor, MDBConfig mdbConfig) {
    var set = new DAOSet();
    UACApisUtil uacApisUtil = new UACApisUtil(executor, services.uac);

    set.metadataDAO = new MetadataDAORdbImpl();
    set.commitDAO = new CommitDAORdbImpl(services.authService, services.mdbRoleService);
    set.repositoryDAO =
        new RepositoryDAORdbImpl(
            services.authService, services.mdbRoleService, set.commitDAO, set.metadataDAO);
    set.blobDAO = new BlobDAORdbImpl(services.authService, services.mdbRoleService);

    set.experimentDAO = new ExperimentDAORdbImpl(services.authService, services.mdbRoleService);
    set.experimentRunDAO =
        new ExperimentRunDAORdbImpl(
            mdbConfig,
            services.authService,
            services.mdbRoleService,
            set.repositoryDAO,
            set.commitDAO,
            set.blobDAO,
            set.metadataDAO);
    set.projectDAO =
        new ProjectDAORdbImpl(
            services.authService, services.mdbRoleService, set.experimentDAO, set.experimentRunDAO);
    if (services.artifactStoreService != null) {
      set.artifactStoreDAO = new ArtifactStoreDAORdbImpl(services.artifactStoreService, mdbConfig);
    } else {
      set.artifactStoreDAO = new ArtifactStoreDAODisabled();
    }

    set.commentDAO = new CommentDAORdbImpl(services.authService);
    set.datasetDAO = new DatasetDAORdbImpl(services.authService, services.mdbRoleService);
    set.lineageDAO = new LineageDAORdbImpl();
    set.datasetVersionDAO =
        new DatasetVersionDAORdbImpl(services.authService, services.mdbRoleService);

    set.futureProjectDAO =
        new FutureProjectDAO(
            executor,
            jdbi,
            services.uac,
            set.artifactStoreDAO,
            set.datasetVersionDAO,
            mdbConfig,
            uacApisUtil);
    set.futureExperimentRunDAO =
        new FutureExperimentRunDAO(
            executor,
            jdbi,
            mdbConfig,
            services.uac,
            set.artifactStoreDAO,
            set.datasetVersionDAO,
            set.repositoryDAO,
            set.commitDAO,
            set.blobDAO,
            uacApisUtil);
    set.futureEventDAO =
        new FutureEventDAO(executor, jdbi, mdbConfig, ServiceEnum.Service.MODELDB_SERVICE.name());

    return set;
  }

  private DAOSet() {}
}
