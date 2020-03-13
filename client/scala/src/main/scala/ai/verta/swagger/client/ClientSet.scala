package ai.verta.swagger.client

import ai.verta.swagger._public.artifactstore.api._
import ai.verta.swagger._public.modeldb.api._

class ClientSet(val client: HttpClient) {
  val artifactStoreService = new ArtifactStoreApi(client, "/api/v1/modeldb")
  val commentService = new CommentApi(client, "/api/v1/modeldb")
  val datasetService = new DatasetServiceApi(client, "/api/v1/modeldb")
  val datasetVersionService = new DatasetVersionServiceApi(client, "/api/v1/modeldb")
  val experimentRunService = new ExperimentRunServiceApi(client, "/api/v1/modeldb")
  val experimentService = new ExperimentServiceApi(client, "/api/v1/modeldb")
  val hydratedService = new HydratedServiceApi(client, "/api/v1/modeldb")
  val jobService = new JobApi(client, "/api/v1/modeldb")
  val lineageService = new LineageApi(client, "/api/v1/modeldb")
  val projectService = new ProjectServiceApi(client, "/api/v1/modeldb")
}
