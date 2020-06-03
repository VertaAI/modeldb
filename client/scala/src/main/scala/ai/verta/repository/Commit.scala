package ai.verta.repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.utils.URLUtils

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

import java.net.URLEncoder

/** Commit within a ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Repository.getCommit methods
 */
class Commit(
  private val clientSet: ClientSet, private val repo: VersioningRepository,
  private val commit: VersioningCommit, private var commitBranch: Option[String] = None
) {
  private var saved = true // whether the commit instance is saved to database, or is currently being modified.

  /** Return the id of the commit */
  def id = commit.commit_sha.get

  override def equals(other: Any) = other match {
    case other: Commit => commit.commit_sha.get == other.commit.commit_sha.get
    case _ => false
  }

  /** Creates a branch at this Commit and returns the checked-out branch
   *  If the branch already exists, it will be moved to this commit.
   *  @param branch branch name
   *  @return if not saved, a failure; otherwise, this commit as the head of `branch`
   */
  def newBranch(branch: String)(implicit ec: ExecutionContext) = {
    if (!saved)
      Failure(new IllegalStateException("Commit must be saved before it can be attached to a branch"))
    else setBranch(branch)
  }

  /** Assigns a tag to this Commit
   *  @param tag tag
   */
  def tag(tag: String)(implicit ec: ExecutionContext) = {
    if (!saved)
      Failure(new IllegalStateException("Commit must be saved before it can be tagged"))
    else clientSet.versioningService.SetTag2(
        body = "\"" + commit.commit_sha.get + "\"",
        repository_id_repo_id = repo.id.get,
        tag = URLUtils.urlEncode(tag)
    ).map(_ => ())
  }

  /** Set the commit of named branch to current commit
   *  @param branch branch
   */
  private def setBranch(branch: String)(implicit ec: ExecutionContext) = {
    clientSet.versioningService.SetBranch2(
      body = "\"" + commit.commit_sha.get + "\"",
      branch = branch,
      repository_id_repo_id = repo.id.get
    ) match {
      case Success(_) => {
        commitBranch = Some(branch)
        Success(())
      }
      case Failure(e) => Failure(e)
    }
  }
}
