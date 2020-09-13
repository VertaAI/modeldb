package ai.verta.dataset_versioning

import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext

import net.liftweb.json._

import ai.verta.blobs.dataset.{PathBlob, S3, S3Location}
import ai.verta.swagger._public.modeldb.model._
import ai.verta.swagger.client.ClientSet

/** Represents a ModelDB dataset.
 */
class Dataset(private val clientSet: ClientSet, private val dataset: ModeldbDataset) {
  /** ID of the dataset. */
  def id: String = dataset.id.get

  /** Name of the dataset. */
  def name: String = dataset.name.get

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
  }

  /** Creates path dataset version.
   *  @param paths Dataset version paths.
   *  @return Dataset version from paths.
   */
  def createPathVersion(paths: List[String])(implicit ec: ExecutionContext): Try[DatasetVersion] =
    PathBlob(paths).flatMap(createVersionFromBlob)

  /** Creates S3 dataset version.
   *  @param paths S3 locations.
   *  @return Dataset version from S3 locations.
   */
  def createS3Version(locations: List[S3Location])(implicit ec: ExecutionContext): Try[DatasetVersion] =
    S3(locations).flatMap(createVersionFromBlob)

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
