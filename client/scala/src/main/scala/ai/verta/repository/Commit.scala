package ai.verta.repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model._

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
  private var saved = true // whether the commit instance is saved to database


  /** Return the id of the commit */
  def getId() = commit.commit_sha.get

  override def equals(other: Any) = other match {
    case other: Commit => commit.commit_sha.get == other.commit.commit_sha.get
    case _ => false
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
        tag = urlEncode(tag)
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

  private def urlEncode(input: String): String = URLEncoder.encode(input, "UTF-8").replaceAll("\\+", "%20")
}
