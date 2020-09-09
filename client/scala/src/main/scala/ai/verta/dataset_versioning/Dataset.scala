package ai.verta.dataset_versioning

import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext

import net.liftweb.json._

import ai.verta.blobs.dataset.{PathBlob, S3, S3Location}
import ai.verta.swagger._public.modeldb.model._
// import ai.verta.swagger._public.modeldb.versioning.model._
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

  /** Creates path dataset version.
   *  @param paths Dataset version paths.
   *  @return Dataset version from paths.
   */
  def createPathVersion(paths: List[String])(implicit ec: ExecutionContext): Try[DatasetVersion] = {
    for (
      pathBlob <- PathBlob(paths);
      response <- clientSet.datasetVersionService.DatasetVersionService_createDatasetVersion(ModeldbCreateDatasetVersion(
        dataset_id = Some(id),
        dataset_blob = Some(convertModel(PathBlob.toVersioningBlob(pathBlob).dataset.get))
      ))
    ) yield new DatasetVersion(clientSet, this, response.dataset_version.get)
  }

  /** Creates S3 dataset version.
   *  @param paths S3 locations.
   *  @return Dataset version from S3 locations.
   */
  def createS3Version(locations: List[S3Location])(implicit ec: ExecutionContext): Try[DatasetVersion] = {
    for (
      s3Blob <- S3(locations);
      response <- clientSet.datasetVersionService.DatasetVersionService_createDatasetVersion(ModeldbCreateDatasetVersion(
        dataset_id = Some(id),
        dataset_blob = Some(convertModel(S3.toVersioningBlob(s3Blob).dataset.get))
      ))
    ) yield new DatasetVersion(clientSet, this, response.dataset_version.get)
  }
}
