package ai.verta.repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.collection.immutable.Map

/** Commit within a ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Repository.getCommit methods
 */
class Commit(
  private val clientSet: ClientSet, private val repo: Repository,
  private val commit: VersioningCommit, private val commitBranch: Option[String] = None
) {
  private var saved = true // whether the commit instance is saved to database, or is currently being modified.
  private var loadedFromRemote = false // whether blobs has been retrieved from remote
  private var blobs = Map[String, VersioningBlob]() // mutable map for storing blobs. Only loaded when used

  /** Return the id of the commit */
  def id = commit.commit_sha

  override def equals(other: Any) = other match {
    case other: Commit => id.isDefined && other.id.isDefined && id.get == other.id.get
    case _ => false
  }

  /** Retrieves the blob at path from this commit
   *  @param path location of a blob
   *  @return The blob. If not existed, or retrieving blobs from backend fails, return a failure.
   */
  def get(path: String)(implicit ec: ExecutionContext): Try[Blob] = {
    loadBlobs().flatMap(_ =>
      blobs.get(path) match {
        case None => Failure(new NoSuchElementException("No blob was stored at this path."))
        case Some(blob) => Success(versioningBlobToBlob(blob))
      }
    )
  }

  /** Adds blob to this commit at path
   *  If path is already in this Commit, it will be updated to the new blob
   *  @param path Location to add blob to
   *  @param blob Instance of Blob subclass.
   *  @return The new commit, if succeeds.
   */
  def update(path: String, blob: Blob)(implicit ec: ExecutionContext): Try[Commit] = {
    loadBlobs().map(_ => {
      // creating new commit:
      val childCommit = getChild()

      /** TODO: Add blob subtypes to pattern matching */
      val versioningBlob = blob match {
        case pathBlob: PathBlob => PathBlob.toVersioningBlob(pathBlob)
        case s3: S3 => S3.toVersioningBlob(s3)
      }

      childCommit.blobs = blobs + (path -> versioningBlob)
      childCommit.saved = false
      childCommit.loadedFromRemote = true

      childCommit
    })
  }

  /** Saves this commit to ModelDB
   *  @param message description of this commit
   *  @return The saved commit, if succeeds.
   */
  def save(message: String)(implicit ec: ExecutionContext) = {
    if (saved)
      Failure(new IllegalStateException("Commit is already saved"))
    else
      blobsList().flatMap(list => createCommit(message = message, blobs = Some(list)))
  }

  /** Helper function to create a new commit and assign to current instance.
   *  @param message the message assigned to the new commit
   *  @param blobs The list of blobs to assign to the new commit (optional)
   *  @param commitBase base for the new commit (optional)
   *  @param diffs a list of diffs (optional)
   */
  private def createCommit(
    message: String,
    blobs: Option[List[VersioningBlobExpanded]] = None,
    commitBase: Option[String] = None,
    diffs: Option[List[VersioningBlobDiff]] = None
  )(implicit ec: ExecutionContext) = {
      clientSet.versioningService.CreateCommit2(
        body = VersioningCreateCommitRequest(
          commit = Some(addMessage(message)),
          blobs = blobs,
          commit_base = commitBase,
          diffs = diffs
        ),
        repository_id_repo_id = repo.id
      )
      .flatMap(r => {
        val newCom = new Commit(clientSet, repo, r.commit.get, commitBranch)

        // Update branch to child commit
        if (commitBranch.isDefined)
          newCom.newBranch(commitBranch.get)
        else
          Success(newCom)
      })
  }

  /** Convert a location to "repeated string" representation
   *  @param path path
   *  @return the repeated string representation of the path
   */
   private def pathToLocation(path: String): List[String] = {
     if (path.startsWith("/")) pathToLocation(path.substring(1))
     else path.split("/").toList
   }

  /** Convert the dictionary of blobs into list form for API requests
    *  @return the list required, if succeeds.
    */
  private def blobsList()(implicit ec: ExecutionContext): Try[List[VersioningBlobExpanded]] = {
    loadBlobs().map(_ => (for ((path, blob) <- blobs) yield VersioningBlobExpanded(
        blob = Some(blob),
        location = Some(pathToLocation(path))
    )).toList)
  }

  /** Add message to current commit. Done before saving
   *  @param message message
   */
  private def addMessage(message: String) = VersioningCommit(
    /** TODO: Deal with author, date_created */
    commit_sha = commit.commit_sha,
    message = Some(message),
    parent_shas = commit.parent_shas
  )

  /** Retrieve commit's blobs from remote
   *  This is only called when user perform operations involving blobs.
   */
  private def loadBlobs()(implicit ec: ExecutionContext): Try[Unit] = {
    if (!loadedFromRemote) {
      // if the commit is not saved, get the blobs of its parent(s)
      val ids: List[String] = commit.commit_sha match {
        case Some(v) => List(v)
        case None => commit.parent_shas.get
      }

      Try(ids.map(id => loadBlobsFromId(id)).map(_.get).reduce(_ ++ _)) match {
        case Failure(e) => Failure(e)
        case Success(map) => Success {
          // the blobs are successfully loaded and assigned to the internal field:
          loadedFromRemote = true
          blobs = map
        }
      }
    }
    else Success(()) // if the blobs are already loaded, ignore
  }


  /** Retrieve blobs associated to commit with given id
   *  @param id id of the commit
   *  @return the blobs, in the form of map from path to blob stored at path
   */
  private def loadBlobsFromId(
    id: String
  )(implicit ec: ExecutionContext): Try[Map[String, VersioningBlob]] = {
    clientSet.versioningService.ListCommitBlobs2(
      commit_sha = id,
      repository_id_repo_id = repo.id
    ) // Try[VersioningListCommitBlobsRequestResponse]
    .map(_.blobs) // Try[Option[List[VersioningBlobExpanded]]]
    .map(ls =>
      if (ls.isEmpty) Map[String, VersioningBlob]()
      else ls.get.map(blob => blob.location.get.mkString("/") -> blob.blob.get).toMap
    )
  }

  /** Return a child commit child of current commit (if current commit is saved)
   *  This helper function is used for modifcation
   */
  private def getChild() = {
    /** TODO: Deal with author, date_created */
    val newCommit = VersioningCommit(
      parent_shas = if (saved) commit.commit_sha.map(List(_)) else commit.parent_shas
    )
    new Commit(clientSet, repo, newCommit, commitBranch)
  }

  /** Helper function to convert a VersioningBlob instance to corresponding Blob subclass instance
   *  @param vb the VersioningBlob instance
   *  @return an instance of a Blob subclass
   */
  def versioningBlobToBlob(vb: VersioningBlob): Blob = vb match {
    /** TODO: finish the pattern matching with other blob subclasses */
    case VersioningBlob(_, _, Some(VersioningDatasetBlob(Some(path), _)), _) => PathBlob(path)
    case VersioningBlob(_, _, Some(VersioningDatasetBlob(_, Some(s3))), _) => S3(s3)
  }

  /** Creates a branch at this Commit and returns the checked-out branch
   *  If the branch already exists, it will be moved to this commit.
   *  @param branch branch name
   *  @return if not saved, a failure; otherwise, this commit as the head of `branch`
   */
  def newBranch(branch: String)(implicit ec: ExecutionContext) = {
    if (!saved)
      Failure(new IllegalStateException("Commit must be saved before it can be attached to a branch"))
    else setBranch(commit, branch).flatMap(_ => repo.getCommitByBranch(branch))
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
  private def setBranch(versioningCommit: VersioningCommit, branch: String)(implicit ec: ExecutionContext) = {
    clientSet.versioningService.SetBranch2(
      body = versioningCommit.commit_sha.get,
      branch = branch,
      repository_id_repo_id = repo.id
    ).map(_ => ())
  }
}
