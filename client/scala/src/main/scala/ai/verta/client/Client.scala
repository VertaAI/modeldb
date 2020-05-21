package ai.verta.client

// TODO: add Scaladoc on top of methods

import ai.verta.client.entities.{Experiment, ExperimentRun, GetOrCreateEntity, Project}
import ai.verta.client.repository.Repository
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
        clientSet.projectService.getProjectByName(name = name, workspace_name = workspace)
          .map(r => if (r.project_by_user.isEmpty) null else new Project(clientSet, r.project_by_user.get))
      },
      create = () => {
        clientSet.projectService.createProject(ModeldbCreateProject(name = Some(name), workspace_name = Some(workspace)))
          .map(r => if (r.project.isEmpty) null else new Project(clientSet, r.project.get))
      })
  }

  def getProject(id: String)(implicit ec: ExecutionContext) = {
    clientSet.projectService.getProjectById(id)
      .map(r => if (r.project.isEmpty) null else new Project(clientSet, r.project.get))
  }

  def getExperiment(id: String)(implicit ec: ExecutionContext) = {
    clientSet.experimentService.getExperimentById(id)
      .flatMap(r => if (r.experiment.isEmpty) Success(null) else Try[Experiment]({
        getProject(r.experiment.get.project_id.get) match {
          case Success(proj) => new Experiment(clientSet, proj, r.experiment.get)
          case Failure(x) => throw x
        }
      }))
  }

  def getExperimentRun(id: String)(implicit ec: ExecutionContext) = {
    clientSet.experimentRunService.getExperimentRunById(id)
      .flatMap(r => if (r.experiment_run.isEmpty) Success(null) else Try[ExperimentRun]({
        getExperiment(r.experiment_run.get.experiment_id.get) match {
          case Success(expt) => new ExperimentRun(clientSet, expt, r.experiment_run.get)
          case Failure(x) => throw x
        }
      }))
  }

  // TODO: implement getOrCreateRepository and getRepository
  // TODO: write tests for them
  /**
  */
  def getOrCreateRepository(name: String, workspace: String = "", id: String = "")(implicit ec: ExecutionContext) = {
    GetOrCreateEntity.getOrCreate[Repository](
      get = () => {
        clientSet.versioningService.GetRepository(
          id_named_id_workspace_name = workspace,
          id_named_id_name = name,
          id_repo_id = BigInt(id)
        )
        .map(r => if (r.repository.isEmpty) null else new Repository(clientSet, r.repository.get))
      },
      create = () => {
        clientSet.versioningService.CreateRepository(
          id_named_id_workspace_name = workspace,
          body = VersioningRepository(
            name = Some(name),
            id = Some(BigInt(id)),
            workspace_id = Some(workspace)
          )
        )
        .map(r => if (r.repository.isEmpty) null else new Repository(clientSet, r.repository.get))
      }
    )
  }

  // def getRepository(id: String)(implicit ec: ExecutionContext) = {
  //   clientSet.versioningService.get
  // }
}
