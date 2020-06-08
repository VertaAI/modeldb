package ai.verta.repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.collection.mutable.HashMap

/** Commit within a ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Repository.getCommit methods
 */
class Commit(
  private val clientSet: ClientSet, private val repo: Repository,
  private var commit: VersioningCommit, private val commitBranch: Option[String] = None
) {
  private var saved = true // whether the commit instance is saved to database, or is currently being modified.
  private var loadedFromRemote = false // whether blobs has been retrieved from remote
  private var blobs = new HashMap[String, VersioningBlob]() // mutable map for storing blobs

  /** Return the id of the commit */
  def id = commit.commit_sha.get

  override def equals(other: Any) = other match {
    case other: Commit => commit.commit_sha.get == other.commit.commit_sha.get
    case _ => false
  }

  /** Retrieves the blob at path from this commit
   *  @param path location of a blob
   *  @return ModelDB versioning blob. If not existed, return None
   */
  def get(path: String)(implicit ec: ExecutionContext): Try[Option[Blob]] = {
    loadBlobs().map(_ => blobs.get(path).map(versioningBlobToBlob))
  }

  /** Adds blob to this commit at path
   *  If path is already in this Commit, it will be updated to the new blob
   *  @param path Location to add blob to
   *  @param blob Instance of Blob subclass.
   *  @return whether the update attempt succeeds.
   */
  def update[T <: Blob](path: String, blob: T)(implicit ec: ExecutionContext): Try[Unit] = {
    loadBlobs().map(_ => {
      becomeChild()

      /** TODO: Add blob subtypes to pattern matching */
      val versioningBlob = blob match {
        case pathBlob: PathBlob => PathBlob.toVersioningBlob(pathBlob)
      }

      blobs.put(path, versioningBlob)
    })
  }

  /** Retrieve commit's blobs from remote
   */
  private def loadBlobs()(implicit ec: ExecutionContext): Try[Unit] = {
    if (!loadedFromRemote) {
      // if the commit is not saved, get the blobs of its parent(s)
      val ids: List[String] = commit.commit_sha match {
        case Some(v) => List(v)
        case None => commit.parent_shas.get
      }

      Try(ids.map(id => loadBlobsFromId(id)).map(_.get).flatten) match {
        case Failure(e) => Failure(e)
        case Success(list) => Success {
          loadedFromRemote = true
          blobs = HashMap(list: _*)
        }
      }
    }
    else Success(())
  }


  /** Retrieve blobs associated to commit with given id and update blobs
   *  @param id id of the commit
   */
  private def loadBlobsFromId(
    id: String
  )(implicit ec: ExecutionContext): Try[List[Tuple2[String, VersioningBlob]]] = {
    clientSet.versioningService.ListCommitBlobs2(
      commit_sha = id,
      repository_id_repo_id = repo.id
    ) // Try[VersioningListCommitBlobsRequestResponse]
    .map(_.blobs) // Try[Option[List[VersioningBlobExpanded]]]
    .map(ls =>
      if (ls.isEmpty) List()
      else ls.get.map(blob => blob.location.get.mkString("/") -> blob.blob.get)
    )
  }

  /** Become child of current commit (if current commit is saved)
   *  This helper function is used before modifcation
   */
  private def becomeChild() = {
    if (saved) {
      commit = VersioningCommit(
        parent_shas = commit.commit_sha.map(List(_))
      )
      saved = false
    }
  }

  /** Helper function to convert a VersioningBlob instance to corresponding Blob subclass instance
   *  @param vb the VersioningBlob instance
   *  @return an instance of a Blob subclass
   */
  def versioningBlobToBlob(vb: VersioningBlob): Blob = vb match {
    /** TODO: finish the pattern matching with other blob subclasses */
    case VersioningBlob(_, _, Some(VersioningDatasetBlob(Some(path), _)), _) => PathBlob(path)
  }

  /** Creates a branch at this Commit and returns the checked-out branch
   *  If the branch already exists, it will be moved to this commit.
   *  @param branch branch name
   *  @return if not saved, a failure; otherwise, this commit as the head of `branch`
   */
  def newBranch(branch: String)(implicit ec: ExecutionContext) = {
    if (!saved)
      Failure(new IllegalStateException("Commit must be saved before it can be attached to a branch"))
    else setBranch(branch).flatMap(_ => repo.getCommitByBranch(branch))
  }

  /** Assigns a tag to this Commit
   *  @param tag tag
   */
  def tag(tag: String)(implicit ec: ExecutionContext) = {
    if (!saved)
      Failure(new IllegalStateException("Commit must be saved before it can be tagged"))
    else clientSet.versioningService.SetTag2(
        body = commit.commit_sha.get,
        repository_id_repo_id = repo.id,
        tag = tag
    ).map(_ => ())
  }

  /** Set the commit of named branch to current commit
   *  @param branch branch
   */
  private def setBranch(branch: String)(implicit ec: ExecutionContext) = {
    clientSet.versioningService.SetBranch2(
      body = commit.commit_sha.get,
      branch = branch,
      repository_id_repo_id = repo.id
    ).map(_ => ())
  }
}
