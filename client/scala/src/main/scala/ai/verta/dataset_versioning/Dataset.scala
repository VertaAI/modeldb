package ai.verta.dataset_versioning

import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext

import ai.verta.client.entities.Taggable
import ai.verta.client.entities.utils._
import ai.verta.swagger._public.modeldb.model._
import ai.verta.swagger.client.ClientSet

/** Represents a ModelDB dataset.
 */
class Dataset(private val clientSet: ClientSet, private val dataset: ModeldbDataset) extends Taggable {
  /** ID of the dataset. */
  def id: String = dataset.id.get

  /** Name of the dataset. */
  def name: String = dataset.name.get

  /** Add a tag to this dataset.
   *  @param tag tag to add.
   */
  def addTag(tag: String)(implicit ec: ExecutionContext): Try[Unit] = addTags(List(tag))

  /** Add multiple tags to this dataset.
   *  @param tags tags to add.
   */
  def addTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit] =
    clientSet.datasetService.DatasetService_addDatasetTags(ModeldbAddDatasetTags(id = Some(id), tags = Some(tags)))
      .map(_ => ())

  /** Delete multiple tags from this dataset.
   *  @param tags tags to delete.
   */
  def delTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit] =
    clientSet.datasetService.DatasetService_deleteDatasetTags(ModeldbDeleteDatasetTags(
      id = Some(id),
      tags = Some(tags)
    ))
      .map(_ => ())

  /** Gets all the tags of this dataset.
   *  @return tags of this dataset.
   */
  def getTags()(implicit ec: ExecutionContext): Try[List[String]] =
    getMessage().map(dataset => dataset.tags.get)

  private def getMessage()(implicit ec: ExecutionContext): Try[ModeldbDataset] =
    clientSet.datasetService.DatasetService_getDatasetById(Some(id))
      .map(r => r.dataset.get)
}
