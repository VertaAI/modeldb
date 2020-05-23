package ai.verta._repository

import ai.verta.client.{getPersonalWorkspace, urlEncode}
import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.collection.mutable.HashMap

/** Commit within a ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Repository.getCommit methods
 *  TODO: privatize blobs
 *  TODO: clean up blobs retrieval
 *  TODO: write tests for interactions with blobs
 */
class Commit(val clientSet: ClientSet, val repo: VersioningRepository, val commit: VersioningCommit) {
  var saved = true // whether the commit instance is saved to database
  var loaded_from_remote = false // whether blob has been retrieved from remote
  var blobs = new HashMap[String, VersioningBlob]() // mutable map for storing blobs

  /** Retrieve blobs
   */
  def load_blobs()(implicit ec: ExecutionContext): Unit = {
    if (!loaded_from_remote) {
      val ids: List[String] = commit.commit_sha match {
        case Some(v) => List(v)
        case None => commit.parent_shas.get
      }

      ids.map(id => load_blobs_from_id(id))
      loaded_from_remote = true
    }
  }

  /** Retrieve blobs associated to commit with given id and update blobs
   *  @param id id of the commit
   */
  private def load_blobs_from_id(id: String)(implicit ec: ExecutionContext): Try[List[List[Option[VersioningBlob]]]] = {
    clientSet.versioningService.ListCommitBlobs2(
      commit_sha = id,
      repository_id_repo_id = repo.id.get
    ) // Try[VersioningListCommitBlobsRequestResponse]
    .map(r => r.blobs) // Try[List[VersioningBlobExpanded]]
    .map(ls => if (ls.isEmpty) null else ls.get.map(
      blob => {
        blob.location.get.map(l => blobs.put(l, blob.blob.get))
      }
    ))
  }

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
