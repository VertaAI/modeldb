package ai.verta.client.entities

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import ai.verta.client.entities.subobjects.Tags
import ai.verta.swagger._public.modeldb.model.{ModeldbAddExperimentTags, ModeldbCreateExperimentRun, ModeldbDeleteExperimentTags, ModeldbExperiment}
import ai.verta.swagger.client.ClientSet

import scala.concurrent.ExecutionContext
import scala.util.Try

class Experiment(val clientSet: ClientSet, val proj: Project, val expt: ModeldbExperiment) extends Taggable {
  def getOrCreateExperimentRun(name: String = "")(implicit ec: ExecutionContext) = {
    val internalName = if (name == "") LocalDateTime.now.format(DateTimeFormatter.ofPattern("YYYY/MM/dd HH:mm:ss.SSSS")) else name

    GetOrCreateEntity.getOrCreate[ExperimentRun](
      get = () => {
        clientSet.experimentRunService.getExperimentRunByName(internalName, expt.id.get)
          .map(r => if (r.experiment_run.isEmpty) null else new ExperimentRun(clientSet, this, r.experiment_run.get))
      },
      create = () => {
        clientSet.experimentRunService.createExperimentRun(ModeldbCreateExperimentRun(
          name = Some(internalName),
          experiment_id = expt.id,
          project_id = proj.proj.id // TODO: remove since we can get from the experiment
        ))
          .map(r => if (r.experiment_run.isEmpty) null else new ExperimentRun(clientSet, this, r.experiment_run.get))
      }
    )
  }

  def tags()(implicit ec: ExecutionContext) = new Tags(clientSet, ec, this)

  override def getTags()(implicit ec: ExecutionContext): Try[List[String]] = {
    clientSet.experimentService.getExperimentTags(expt.id.get)
      .map(r => r.tags.getOrElse(Nil))
  }

  override def delTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit] = {
    clientSet.experimentService.deleteExperimentTags(ModeldbDeleteExperimentTags(
      id = expt.id,
      tags = Some(tags)
    ))
      .map(_ => {})
  }

  override def addTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit] = {
    clientSet.experimentService.addExperimentTags(ModeldbAddExperimentTags(
      id = expt.id,
      tags = Some(tags)
    ))
      .map(_ => {})
  }
}
