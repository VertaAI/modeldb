package ai.verta.client

import ai.verta.client.entities.{Experiment, ExperimentRun, GetOrCreateEntity, Project}
import ai.verta.repository.Repository
import ai.verta.swagger._public.modeldb.model.{ModeldbCreateProject, ModeldbDeleteProject}
import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.swagger.client.{ClientSet, HttpClient}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class Client(conn: ClientConnection) {
  val httpClient = new HttpClient(conn.host, Map(
    "Grpc-Metadata-email" -> conn.auth.email,
    "Grpc-Metadata-developer_key" -> conn.auth.devKey,
    "Grpc-Metadata-source" -> "PythonClient"
  ))

  val clientSet = new ClientSet(httpClient)

  def close() = httpClient.close()

  def getOrCreateProject(name: String, workspace: String = "")(implicit ec: ExecutionContext) = {
    GetOrCreateEntity.getOrCreate[Project](
      get = () => {
        clientSet.projectService.ProjectService_getProjectByName(name = Some(name), workspace_name = Some(workspace))
          .map(r => new Project(clientSet, r.project_by_user.get))
      },
      create = () => {
        clientSet.projectService.ProjectService_createProject(ModeldbCreateProject(name = Some(name), workspace_name = Some(workspace)))
          .map(r => new Project(clientSet, r.project.get))
      })
  }

  def getProject(id: String)(implicit ec: ExecutionContext) = {
    clientSet.projectService.ProjectService_getProjectById(Some(id))
      .map(r => new Project(clientSet, r.project.get))
  }

  /** Delete the project with given id
   *  @param id id of the project to be deleted
   *  @return whether the delete attempt suceeds
   */
  def deleteProject(id: String)(implicit ec: ExecutionContext) = {
    clientSet.projectService.ProjectService_deleteProject(ModeldbDeleteProject(Some(id)))
      .map(_ => ())
  }

  def getExperiment(id: String)(implicit ec: ExecutionContext) = {
    clientSet.experimentService.ExperimentService_getExperimentById(Some(id))
      .flatMap(r => Try[Experiment]({
        getProject(r.experiment.get.project_id.get) match {
          case Success(proj) => new Experiment(clientSet, proj, r.experiment.get)
          case Failure(x) => throw x
        }
      }))
  }

  def getExperimentRun(id: String)(implicit ec: ExecutionContext) = {
    clientSet.experimentRunService.ExperimentRunService_getExperimentRunById(Some(id))
      .flatMap(r => Try[ExperimentRun]({
        getExperiment(r.experiment_run.get.experiment_id.get) match {
          case Success(expt) => new ExperimentRun(clientSet, expt, r.experiment_run.get)
          case Failure(x) => throw x
        }
      }))
  }

  /** Get the repository by name (and workspace). If not exist, create new repository
   * @param name Name of the Repository
   * @param workspace Workspace under which the Repository with name name exists. If not provided, the current user’s personal workspace will be used.
   */
  def getOrCreateRepository(name: String, workspace: Option[String] = None)(implicit ec: ExecutionContext) = {
    GetOrCreateEntity.getOrCreate[Repository](
      get = () => {
        clientSet.versioningService.VersioningService_GetRepository(
          id_named_id_workspace_name = workspace.getOrElse(getPersonalWorkspace()),
          id_named_id_name = name
        ).map(r => new Repository(clientSet, r.repository.get))
      },
      create = () => {
        clientSet.versioningService.VersioningService_CreateRepository(
          id_named_id_workspace_name = workspace.getOrElse(getPersonalWorkspace()),
          body = VersioningRepository(
            name = Some(name),
            workspace_id = workspace
          )
        ).map(r => new Repository(clientSet, r.repository.get))
      }
    )
  }

  /** Get repository based on id
   *  @param id id of the repository
   *  @return the repository
   */
  def getRepository(id: BigInt)(implicit ec: ExecutionContext): Try[Repository] = {
    clientSet.versioningService.VersioningService_GetRepository2(
      id_repo_id = id
    ).map(r => new Repository(clientSet, r.repository.get))
  }


  /** Delete repository based on id
   *  @param id id of the repository
   */
  def deleteRepository(id: BigInt)(implicit ec: ExecutionContext): Try[Unit] = {
    clientSet.versioningService.VersioningService_DeleteRepository2(
      repository_id_repo_id = id
    ).map(_ => ())
  }

  /** Get the user's personal workspace. Currently, only returns "personal"
   */
  private def getPersonalWorkspace()(implicit ec: ExecutionContext): String = {
    "personal"
  }
}
