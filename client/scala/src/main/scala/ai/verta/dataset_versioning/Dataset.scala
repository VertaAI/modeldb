package ai.verta.dataset_versioning

import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext

import ai.verta.client.entities.Taggable
import ai.verta.client.entities.utils._
import ai.verta.client.entities.utils._
import net.liftweb.json._

import ai.verta.blobs.dataset.{PathBlob, S3, S3Location, DBDatasetBlob, AtlasHiveDatasetBlob, QueryDatasetBlob}
import ai.verta.swagger._public.modeldb.model._
import ai.verta.swagger.client.ClientSet

/** Represents a ModelDB dataset.
 */
class Dataset(private val clientSet: ClientSet, private val dataset: ModeldbDataset) extends Taggable {
  /** ID of the dataset. */
  def id: String = dataset.id.get

  /** Name of the dataset. */
  def name: String = dataset.name.get

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

  private def convertModel(
    datasetMessage: ai.verta.swagger._public.modeldb.versioning.model.VersioningDatasetBlob
  ): VersioningDatasetBlob = modelFromJson(modelToJson(datasetMessage))

  private def modelToJson: ai.verta.swagger._public.modeldb.versioning.model.VersioningDatasetBlob => JObject =
    ai.verta.swagger._public.modeldb.versioning.model.VersioningDatasetBlob.toJson _

  private def modelFromJson: JObject => VersioningDatasetBlob =
    VersioningDatasetBlob.fromJson _

  private def createVersionFromBlob(blob: ai.verta.blobs.dataset.Dataset)(implicit ec: ExecutionContext): Try[DatasetVersion] =
    clientSet.datasetVersionService.DatasetVersionService_createDatasetVersion(ModeldbCreateDatasetVersion(
      dataset_id = Some(id),
      dataset_blob = Some(convertModel(blobToVersioningDatasetBlob(blob)))
    ))
      .map(response => new DatasetVersion(clientSet, this, response.dataset_version.get))

  private def blobToVersioningDatasetBlob(blob: ai.verta.blobs.dataset.Dataset) = blob match {
    case s3Blob: S3 => S3.toVersioningBlob(s3Blob).dataset.get
    case pathBlob: PathBlob => PathBlob.toVersioningBlob(pathBlob).dataset.get
    case queryDatasetBlob: QueryDatasetBlob => QueryDatasetBlob.toVersioningBlob(queryDatasetBlob).dataset.get
  }

  /** Creates a path dataset version.
   *  @param paths Dataset version paths.
   *  @return Dataset version from paths.
   */
  def createPathVersion(paths: List[String])(implicit ec: ExecutionContext): Try[DatasetVersion] =
    PathBlob(paths).flatMap(createVersionFromBlob)

  /** Creates a S3 dataset version.
   *  @param paths S3 locations.
   *  @return Dataset version from S3 locations.
   */
  def createS3Version(locations: List[S3Location])(implicit ec: ExecutionContext): Try[DatasetVersion] =
    S3(locations).flatMap(createVersionFromBlob)

  /** Creates a database dataset version.
   *  @param query database query
   *  @param  dbConnectionStr connection to database.
   *  @param numRecords number of records of the dataset.
   *  @param executionTimestamp timestamp of the query execution.
   */
  def createDBVersion(
    query: String,
    dbConnectionStr: String,
    numRecords: Option[BigInt] = None,
    executionTimestamp: Option[BigInt] = None
  )(implicit ec: ExecutionContext) =
    createVersionFromBlob(DBDatasetBlob(query, dbConnectionStr, numRecords, executionTimestamp))

  /** Creates a dataset version from an Atlas Hive table query.
   *  @param guid guid of the table
   *  @param atlasURL Atlas url. Picked up from environment by default.
   *  @param atlasUserName Atlas user name. Picked up from environment by default.
   *  @param atlasPassword Atlas password. Picked up from environment by default.
   *  @param atlasEntityEndpoint Atlas endpoint to query.
   */
  def createAtlasHiveVersion(
    guid: String,
    atlasURL: String = sys.env.get("ATLAS_URL").getOrElse(""),
    atlasUserName: String = sys.env.get("ATLAS_USERNAME").getOrElse(""),
    atlasPassword: String = sys.env.get("ATLAS_PASSWORD").getOrElse(""),
    atlasEntityEndpoint: String = "/api/atlas/v2/entity/bulk"
  )(implicit ec: ExecutionContext) =
    for (
      blob <- AtlasHiveDatasetBlob(guid, atlasURL, atlasUserName, atlasPassword, atlasEntityEndpoint);
      datasetVersion <- createVersionFromBlob(blob);
      // skip adding tags and attributes if they are empty:
      _ <- if (blob.tags.isEmpty) Success(()) else datasetVersion.addTags(blob.tags);
      _ <- if (blob.attributes.isEmpty) Success(()) else datasetVersion.addAttributes(blob.attributes)
    ) yield datasetVersion

  /** Gets a version of the dataset by its ID.
   *  @param id ID of the dataset version.
   *  @return the version with the given ID.
   */
  def getVersion(id: String)(implicit ec: ExecutionContext): Try[DatasetVersion] =
    clientSet.datasetVersionService.DatasetVersionService_getDatasetVersionById(Some(id)).map(
      response => new DatasetVersion(clientSet, this, response.dataset_version.get)
    )

  /** Gets the latest dataset version.
   *  @return the latest version of this dataset.
   */
  def getLatestVersion()(implicit ec: ExecutionContext): Try[DatasetVersion] =
    clientSet.datasetVersionService.DatasetVersionService_getLatestDatasetVersionByDatasetId(
      dataset_id = Some(id)
    )
      .map(response => new DatasetVersion(clientSet, this, response.dataset_version.get))
}
