package ai.verta.modeldb;

import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAODisabled;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAORdbImpl;
import ai.verta.modeldb.audit_log.AuditLogLocalDAO;
import ai.verta.modeldb.audit_log.AuditLogLocalDAODisabled;
import ai.verta.modeldb.audit_log.AuditLogLocalDAORdbImpl;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.comment.CommentDAO;
import ai.verta.modeldb.comment.CommentDAORdbImpl;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experiment.ExperimentDAORdbImpl;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAORdbImpl;
import ai.verta.modeldb.lineage.LineageDAO;
import ai.verta.modeldb.lineage.LineageDAORdbImpl;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataDAORdbImpl;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.project.ProjectDAORdbImpl;
import ai.verta.modeldb.versioning.*;

public class DAOSet {
  AuditLogLocalDAO auditLogLocalDAO;
  ArtifactStoreDAO artifactStoreDAO;
  BlobDAO blobDAO;
  CommentDAO commentDAO;
  CommitDAO commitDAO;
  ExperimentDAO experimentDAO;
  ExperimentRunDAO experimentRunDAO;
  LineageDAO lineageDAO;
  MetadataDAO metadataDAO;
  ProjectDAO projectDAO;
  RepositoryDAO repositoryDAO;

  public static DAOSet fromServices(ServiceSet services) {
    DAOSet set = new DAOSet();

    set.metadataDAO = new MetadataDAORdbImpl();
    set.commitDAO = new CommitDAORdbImpl(services.authService, services.roleService);
    set.repositoryDAO =
        new RepositoryDAORdbImpl(
            services.authService, services.roleService, set.commitDAO, set.metadataDAO);
    set.blobDAO = new BlobDAORdbImpl(services.authService, services.roleService);

    set.experimentDAO = new ExperimentDAORdbImpl(services.authService, services.roleService);
    set.experimentRunDAO =
        new ExperimentRunDAORdbImpl(
            services.authService,
            services.roleService,
            set.repositoryDAO,
            set.commitDAO,
            set.blobDAO,
            set.metadataDAO);
    set.projectDAO =
        new ProjectDAORdbImpl(
            services.authService, services.roleService, set.experimentDAO, set.experimentRunDAO);
    if (services.artifactStoreService != null) {
      set.artifactStoreDAO = new ArtifactStoreDAORdbImpl(services.artifactStoreService);
    } else {
      set.artifactStoreDAO = new ArtifactStoreDAODisabled();
    }

    set.commentDAO = new CommentDAORdbImpl(services.authService);
    set.lineageDAO = new LineageDAORdbImpl();
    if (services.authService instanceof PublicAuthServiceUtils) {
      set.auditLogLocalDAO = new AuditLogLocalDAODisabled();
    } else {
      set.auditLogLocalDAO = new AuditLogLocalDAORdbImpl();
    }

    return set;
  }

  private DAOSet() {}
}
