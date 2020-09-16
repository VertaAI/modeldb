package ai.verta.dataset_versioning

import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext

import ai.verta.client.entities.Taggable
import ai.verta.blobs.dataset.PathBlob
import ai.verta.client.entities.utils._
import ai.verta.swagger._public.modeldb.model._
import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.swagger.client.ClientSet

/** Represents a ModelDB dataset version.
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
    getMessage().map(dataset_version => dataset_version.tags.getOrElse(Nil))

  /** Adds potentially multiple attributes to this dataset version.
   *  @param vals Attributes name and value (String, Int, or Double)
   */
  def addAttributes(vals: Map[String, ValueType])(implicit ec: ExecutionContext): Try[Unit] = {
    val valsList = KVHandler.mapToKVList(vals)
    if (valsList.isFailure) Failure(valsList.failed.get) else
      clientSet.datasetVersionService.DatasetVersionService_addDatasetVersionAttributes(ModeldbAddDatasetVersionAttributes(
        id = Some(id),
        attributes = valsList.toOption
      )).map(_ => {})
  }

  /** Adds an attribute to this dataset version.
   *  @param key Name of the attribute
   *  @param value Value of the attribute. Could be String, Int, or Double
   */
  def addAttribute(key: String, value: ValueType)(implicit ec: ExecutionContext) =
    addAttributes(Map(key -> value))

  /** Gets all attributes of this dataset version.
   *  @return All the attributes (String, Int, or Double)
   */
  def getAttributes()(implicit ec: ExecutionContext): Try[Map[String, ValueType]] = {
    getMessage()
      .flatMap(dataset_version => {
        if (dataset_version.attributes.isEmpty)
          Success(Map[String, ValueType]())
        else
          KVHandler.kvListToMap(dataset_version.attributes.get)
      })
  }

  /** Get attribute with the given key of this dataset version.
   *  @param key key of the attribute.
   @  @return value stored at the attribute.
   */
  def getAttribute(key: String)(implicit ec: ExecutionContext): Try[Option[ValueType]] =
    getAttributes().map(attributes => attributes.get(key))

  // get the latest version of the proto message
  private def getMessage()(implicit ec: ExecutionContext): Try[ModeldbDatasetVersion] =
    clientSet.datasetVersionService.DatasetVersionService_getDatasetVersionById(Some(id)).map(
      response => response.dataset_version.get
    )
}
