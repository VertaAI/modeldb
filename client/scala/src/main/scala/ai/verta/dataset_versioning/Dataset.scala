package ai.verta.dataset_versioning

import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext

import ai.verta.client.entities.utils._
import ai.verta.swagger._public.modeldb.model._
import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.swagger.client.ClientSet

/** Represents a ModelDB dataset.
 */
class Dataset(private val clientSet: ClientSet, private val dataset: ModeldbDataset) {
  /** ID of the dataset. */
  def id: String = dataset.id.get

  /** Name of the dataset. */
  def name: String = dataset.name.get

  // TODO: add overwrite
  /** Adds potentially multiple attributes to this Dataset.
   *  @param vals Attributes name and value (String, Int, or Double)
   */
  def addAttributes(vals: Map[String, ValueType])(implicit ec: ExecutionContext): Try[Unit] = {
    val valsList = KVHandler.mapToKVList(vals)
    if (valsList.isFailure) Failure(valsList.failed.get) else
      clientSet.datasetService.DatasetService_addDatasetAttributes(ModeldbAddDatasetAttributes(
        id = Some(id),
        attributes = valsList.toOption
      )).map(_ => {})
  }

  /** Adds an attribute to this Dataset.
   *  @param key Name of the attribute
   *  @param value Value of the attribute. Could be String, Int, or Double
   */
  def addAttribute(key: String, value: ValueType)(implicit ec: ExecutionContext) =
    addAttributes(Map(key -> value))

  /** Gets all attributes of this Dataset.
   *  @return All the attributes (String, Int, or Double)
   */
  def getAttributes()(implicit ec: ExecutionContext): Try[Map[String, ValueType]] = {
    clientSet.datasetService.DatasetService_getDatasetById(Some(id))
      .map(r => r.dataset.get)
      .flatMap(dataset => {
        if (dataset.attributes.isEmpty)
          Success(Map[String, ValueType]())
        else
          KVHandler.kvListToMap(dataset.attributes.get)
      })
  }

  /** Get attribute with the given key of this Dataset.
   *  @param key key of the attribute.
   @  @return value stored at the attribute.
   */
  def getAttribute(key: String)(implicit ec: ExecutionContext): Try[Option[ValueType]] =
    getAttributes().map(attributes => attributes.get(key))
}
