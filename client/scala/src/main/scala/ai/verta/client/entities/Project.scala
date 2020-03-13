package ai.verta.client.entities

import ai.verta.client.entities.subobjects.Tags
import ai.verta.swagger._public.modeldb.model.{ModeldbAddProjectTags, ModeldbCreateExperiment, ModeldbDeleteProjectTags, ModeldbProject}
import ai.verta.swagger.client.ClientSet

import scala.concurrent.ExecutionContext
import scala.util.Try

class Project(val clientSet: ClientSet, val proj: ModeldbProject) extends Taggable {
  def getOrCreateExperiment(name: String)(implicit ec: ExecutionContext) = {
    GetOrCreateEntity.getOrCreate[Experiment](
      get = () => {
        clientSet.experimentService.getExperimentByName(name, proj.id.get)
          .map(r => if (r.experiment.isEmpty) null else new Experiment(clientSet, this, r.experiment.get))
      },
      create = () => {
        clientSet.experimentService.createExperiment(ModeldbCreateExperiment(
          name = Some(name),
          project_id = proj.id
        ))
          .map(r => if (r.experiment.isEmpty) null else new Experiment(clientSet, this, r.experiment.get))
      }
    )
  }

  def tags()(implicit ec: ExecutionContext) = new Tags(clientSet, ec, this)

  override def getTags()(implicit ec: ExecutionContext): Try[List[String]] = {
    clientSet.projectService.getProjectTags(proj.id.get)
      .map(r => r.tags.getOrElse(Nil))
  }

  override def delTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit] = {
    clientSet.projectService.deleteProjectTags(ModeldbDeleteProjectTags(
      id = proj.id,
      tags = Some(tags)
    ))
      .map(_ => {})
  }

  override def addTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit] = {
    clientSet.projectService.addProjectTags(ModeldbAddProjectTags(
      id = proj.id,
      tags = Some(tags)
    ))
      .map(_ => {})
  }
}
