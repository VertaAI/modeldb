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
  private val clientSet: ClientSet,
  private val repo: Repository,
  private val commit: VersioningCommit,
  private val commitBranch: Option[String] = None,
  private val saved: Boolean = true
) {
  private var blobs = Map[String, VersioningBlob]()

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
      blobs.get(path) match {
        case None => Failure(new NoSuchElementException("No blob was stored at this path."))
        case Some(blob) => Success(versioningBlobToBlob(blob))
      }
  }

  /** Adds blob to this commit at path
   *  If path is already in this Commit, it will be updated to the new blob
   *  @param path Location to add blob to
   *  @param blob Instance of Blob subclass.
   *  @return The new commit, if succeeds.
   */
  def update(path: String, blob: Blob)(implicit ec: ExecutionContext): Try[Commit] = {
    // creating new commit:
    val childCommit = getChild()

    /** TODO: Add blob subtypes to pattern matching */
    val versioningBlob = blob match {
      case pathBlob: PathBlob => PathBlob.toVersioningBlob(pathBlob)
      case s3: S3 => S3.toVersioningBlob(s3)
    }

    Commit(clientSet, repo, childCommit, commitBranch, false, Some(blobs + (path -> versioningBlob)))
  }

  /** Return a child commit child of current commit (if current commit is saved)
   *  This helper function is used for modifcation
   *  TODO: Deal with author, date_created
   */
  private def getChild() = VersioningCommit(
      parent_shas = if (saved) commit.commit_sha.map(List(_)) else commit.parent_shas
    )

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

object Commit {
  def apply(
    clientSet: ClientSet,
    repo: Repository,
    versioningCommit: VersioningCommit,
    commitBranch: Option[String] = None,
    saved: Boolean = true,
    blobs: Option[Map[String, VersioningBlob]] = None
  )(implicit ec: ExecutionContext) = Try {
    // wrap in Try because loading blobs from DB might fail
    var commit = new Commit(clientSet, repo, versioningCommit, commitBranch, saved)
    commit.blobs = blobs.getOrElse(loadBlobs(clientSet, repo, versioningCommit).get)
    commit
  }

  /** Retrieve commit's blobs from remote
   *  If the commit is saved, retrieve the stored blobs; otherwise retrieve the blobs of its parent(s).
   *  @param clientSet client set
   *  @param repo commit's repository
   *  @param versioningCommit versioning commit instance of the commit
   *  @return The blobs, in the form of map from path to blob.
   */
  private def loadBlobs(
    clientSet: ClientSet,
    repo: Repository,
    versioningCommit: VersioningCommit
  )(implicit ec: ExecutionContext): Try[Map[String, VersioningBlob]] = {
      // if the commit is not saved, get the blobs of its parent(s)
      val ids: List[String] = versioningCommit.commit_sha match {
        case Some(v) => List(v)
        case None => versioningCommit.parent_shas.get
      }

      Try(ids.map(id => loadBlobsFromId(clientSet, repo, id)).map(_.get).reduce(_ ++ _))
    }


  /** Retrieve blobs associated to a commit with given id and construct the map
   *  @param clientSet client set
   *  @param repo commit's repository
   *  @param id id of the commit
   *  @return The blobs, in the form of map from path to blob.
   */
  private def loadBlobsFromId(
    clientSet: ClientSet,
    repo: Repository,
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
}
