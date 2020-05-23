package ai.verta.client

// TODO: add Scaladoc on top of methods
// TODO: write tests for getOrCreateRepository and getRepository

import ai.verta.client.entities.{Experiment, ExperimentRun, GetOrCreateEntity, Project}
import ai.verta._repository.Repository
import ai.verta.swagger._public.modeldb.model.ModeldbCreateProject
import ai.verta.swagger._public.modeldb.versioning.model.VersioningRepository
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
        clientSet.projectService.getProjectByName(name = Some(name), workspace_name = Some(workspace))
          .map(r => if (r.project_by_user.isEmpty) null else new Project(clientSet, r.project_by_user.get))
      },
      create = () => {
        clientSet.projectService.createProject(ModeldbCreateProject(name = Some(name), workspace_name = Some(workspace)))
          .map(r => if (r.project.isEmpty) null else new Project(clientSet, r.project.get))
      })
  }

  def getProject(id: String)(implicit ec: ExecutionContext) = {
    clientSet.projectService.getProjectById(Some(id))
      .map(r => if (r.project.isEmpty) null else new Project(clientSet, r.project.get))
  }

  def getExperiment(id: String)(implicit ec: ExecutionContext) = {
    clientSet.experimentService.getExperimentById(Some(id))
      .flatMap(r => if (r.experiment.isEmpty) Success(null) else Try[Experiment]({
        getProject(r.experiment.get.project_id.get) match {
          case Success(proj) => new Experiment(clientSet, proj, r.experiment.get)
          case Failure(x) => throw x
        }
      }))
  }

  def getExperimentRun(id: String)(implicit ec: ExecutionContext) = {
    clientSet.experimentRunService.getExperimentRunById(Some(id))
      .flatMap(r => if (r.experiment_run.isEmpty) Success(null) else Try[ExperimentRun]({
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
  def getOrCreateRepository(name: String, workspace: String = null)(implicit ec: ExecutionContext) = {
    GetOrCreateEntity.getOrCreate[Repository](
      get = () => {
        clientSet.versioningService.GetRepository(
          id_named_id_workspace_name = if (workspace != null) workspace else getPersonalWorkspace(),
          id_named_id_name = urlEncode(name)
        )
        .map(r => if (r.repository.isEmpty) null else new Repository(clientSet, r.repository.get))
      },
      create = () => {
        clientSet.versioningService.CreateRepository(
          id_named_id_workspace_name = if (workspace != null) workspace else getPersonalWorkspace(),
          body = VersioningRepository(
            name = Some(name),
            workspace_id = Some(workspace) // call getPersonalWorkspace for this?
          )
        )
        .map(r => if (r.repository.isEmpty) null else new Repository(clientSet, r.repository.get))
      }
    )
  }

  /** Get repository based on id
   *  @param id id of the repository
   *  @return the repository
   */
  def getRepository(id: String)(implicit ec: ExecutionContext): Try[Repository] = {
    clientSet.versioningService.GetRepository2(
      id_repo_id = BigInt(id)
    )
    .map(r => if (r.repository.isEmpty) null else new Repository(clientSet, r.repository.get))
  }
}
