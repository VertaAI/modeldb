package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.blobs._

/** Captures metadata about a dataset from a RMDBS database query.
 */
case class RDBMSDatasetBlob(
  val rdbmsQuery: String,
  val dbConnectionStr: String,
  override val numRecords: Option[BigInt] = None,
  override val executionTimestamp: Option[BigInt] = None
) extends QueryDatasetBlob {
  override val dataSourceURI: Option[String] = Some(dbConnectionStr)
  override val query: Option[String] = Some(rdbmsQuery)
}
