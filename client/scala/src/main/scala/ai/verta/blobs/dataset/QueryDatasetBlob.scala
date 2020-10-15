package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.blobs._

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext

/** Captures metadata about a dataset from a query.
 */
trait QueryDatasetBlob extends Dataset {
  val query: Option[String] = None
  val dataSourceURI: Option[String] = None
  val executionTimestamp: Option[BigInt] = None
  val numRecords: Option[BigInt] = None

  // These fields/methods inherited from Dataset are not used
  // TODO: Create another level of abstraction between Dataset and PathBlob + S3 for these
  val enableMDBVersioning: Boolean = false
  val downloadable: Boolean = false
  protected val contents: HashMap[String, FileMetadata] = new HashMap()

  private[verta] def prepareForUpload(): Try[Unit] =
    Failure(new IllegalArgumentException("Query dataset does not support downloading."))

  protected def component = VersioningQueryDatasetComponentBlob(
    query = query,
    num_records = numRecords,
    data_source_uri = dataSourceURI,
    execution_timestamp = executionTimestamp
  )
}

object QueryDatasetBlob {
  /** Factory method to convert a versioning query dataset blob instance. Not meant to be used by user
   *  @param queryVersioningBlob the versioning blob to convert
   *  @return equivalent QueryDatasetBlob instance
   */
  def apply(queryVersioningBlob: VersioningQueryDatasetBlob) = new QueryDatasetBlob {
    private val extractedComponent = queryVersioningBlob.components.get.head // always exists.

    override val query = extractedComponent.query
    override val dataSourceURI = extractedComponent.data_source_uri
    override val executionTimestamp = extractedComponent.execution_timestamp
    override val numRecords = extractedComponent.num_records
  }

  /** Convert a QueryDatasetBlob instance to a VersioningBlob
   *  @param blob QueryDatasetBlob instance
   *  @return equivalent VersioningBlob instance
   */
  def toVersioningBlob(blob: QueryDatasetBlob) = VersioningBlob(
    dataset = Some(VersioningDatasetBlob(
      query = Some(VersioningQueryDatasetBlob(Some(List(blob.component))))
    ))
  )
}
