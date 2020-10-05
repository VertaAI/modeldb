package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.versioning.RepositoryDAO;

public class OrganizationResourceDAOs {

  private final ProjectDAO projectDAO;
  private final RepositoryDAO repositoryDAO;
  private final ExperimentRunDAO experimentRunDAO;

  public OrganizationResourceDAOs(
      ProjectDAO projectDAO, RepositoryDAO repositoryDAO, ExperimentRunDAO experimentRunDAO) {
    this.projectDAO = projectDAO;
    this.repositoryDAO = repositoryDAO;
    this.experimentRunDAO = experimentRunDAO;
  }

  public ProjectDAO getProjectDAO() {
    return projectDAO;
  }

  public RepositoryDAO getRepositoryDAO() {
    return repositoryDAO;
  }

  public ExperimentRunDAO getExperimentRunDAO() {
    return experimentRunDAO;
  }
}
