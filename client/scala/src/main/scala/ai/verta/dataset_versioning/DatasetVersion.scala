package ai.verta.dataset_versioning

import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext

import ai.verta.client.entities.Taggable
import ai.verta.blobs.dataset.PathBlob
import ai.verta.client.entities.utils._
import ai.verta.swagger._public.modeldb.model._
import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.swagger.client.ClientSet

/** Represents a ModelDB dataset version from path.
 */
class DatasetVersion(
  private val clientSet: ClientSet,
  private val dataset: Dataset,
  private val datasetVersion: ModeldbDatasetVersion
) extends Taggable {
  /** ID of the dataset version. */
  def id = datasetVersion.id.get

  // TODO: add overwrite
  /** Add tags to this dataset version.
   *  @param tags tags to add.
   */
  def addTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit] =
    clientSet.datasetVersionService.DatasetVersionService_addDatasetVersionTags(ModeldbAddDatasetVersionTags(
      id = Some(id),
      tags = Some(tags))
    )
      .map(_ => ())

  /** Add a tag to this version.
   *  @param tag tag to add.
   */
  def addTag(tag: String)(implicit ec: ExecutionContext): Try[Unit] = addTags(List(tag))

  /** Delete tags from this dataset version.
   *  @param tags tags to delete.
   */
  def delTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit] =
    clientSet.datasetVersionService.DatasetVersionService_deleteDatasetVersionTags(ModeldbDeleteDatasetVersionTags(
      id = Some(id),
      tags = Some(tags)
    ))
      .map(_ => ())

  /** Gets all the tags of this dataset version.
   *  @return tags of this dataset version.
   */
  def getTags()(implicit ec: ExecutionContext): Try[List[String]] =
    getMessage().map(dataset_version => dataset_version.tags.get)
}
