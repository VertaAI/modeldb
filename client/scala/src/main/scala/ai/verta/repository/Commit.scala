package ai.verta.repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/** Commit within a ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Repository.getCommit methods
 */
class Commit(
  private val clientSet: ClientSet, private val repo: VersioningRepository,
  private val commit: VersioningCommit, private val commitBranch: Option[String] = None
) {

  /** Return the id of the commit */
  def getId() = commit.commit_sha.get

  override def equals(other: Any) = other match {
    case other: Commit => commit.commit_sha.get == other.commit.commit_sha.get
    case _ => false
  }
}
