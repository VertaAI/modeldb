package ai.verta._repository

import ai.verta.client.{getPersonalWorkspace, urlEncode}
import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model.{VersioningCommit, VersioningRepository}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/** Commit within a ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Repository.getCommit methods
 *  TODO: Incorporate blobs
 */
class Commit(val clientSet: ClientSet, val repo: VersioningRepository, val commit: VersioningCommit) {
  var saved = true // whether the commit instance is saved to database

  /** Return ancestors, starting from this Commit until the root of the Repository
   *  @return a list of ancestors
   */
  def log()(implicit ec: ExecutionContext): Try[List[Commit]] = {
    clientSet.versioningService.ListCommitsLog4(
      repository_id_repo_id = repo.id.get,
      commit_sha = commit.commit_sha.get
    ) // Try[VersioningListCommitsLogRequestResponse]
    .map(r => r.commits) // Try[Option[List[VersioningCommit]]]
    .map(ls => if (ls.isEmpty) null else ls.get.map(c => new Commit(clientSet, repo, c))) // Try[List[Commit]]
  }

  /** Assigns a tag to this Commit
   *  @param tag tag
   */
  def tag(tag: String)(implicit ec: ExecutionContext) = {
    if (!saved) {
      throw new IllegalStateException("Commit must be saved before it can be tagged")
    }
    else {
      clientSet.versioningService.SetTag2(
        body = "\"" + commit.commit_sha.get + "\"",
        repository_id_repo_id = repo.id.get,
        tag = urlEncode(tag)
      )
    }
  }
}
