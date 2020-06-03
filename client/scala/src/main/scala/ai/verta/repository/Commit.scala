package ai.verta.repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/** Commit within a ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Repository.getCommit methods
 */
class Commit(
  val clientSet: ClientSet, val repo: VersioningRepository,
  val commit: VersioningCommit, val commitBranch: Option[String] = None
) {
}
