package ai.verta._repository

import ai.verta.client.{getPersonalWorkspace, urlEncode}
import ai.verta.blobs._
import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.collection.mutable.HashMap

/** Commit within a ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Repository.getCommit methods
 *  TODO: clean up blobs retrieval
 *  TODO: improve createCommit
 */
class Commit(
  val clientSet: ClientSet, val repo: VersioningRepository,
  var commit: VersioningCommit, var commit_branch: Option[String] = None
) {
  var saved = true // whether the commit instance is saved to database
  var loaded_from_remote = false // whether blob has been retrieved from remote
  var blobs = new HashMap[String, VersioningBlob]() // mutable map for storing blobs

  /** Retrieve commit's blobs from remote
   */
  private def load_blobs()(implicit ec: ExecutionContext): Unit = {
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
  private def load_blobs_from_id(id: String)(implicit ec: ExecutionContext): Try[List[Option[VersioningBlob]]] = {
    clientSet.versioningService.ListCommitBlobs2(
      commit_sha = id,
      repository_id_repo_id = repo.id.get
    ) // Try[VersioningListCommitBlobsRequestResponse]
    .map(_.blobs) // Try[Option[List[VersioningBlobExpanded]]]
    .map(ls => if (ls.isEmpty) null else ls.get.map(
      blob => {
        var joinedLocation = blob.location.get.mkString("/")
        blobs.put(joinedLocation, blob.blob.get)
      }
    ))
  }

  /** Retrieves the blob at path from this commit
   *  @param path location of a blob
   *  @return ModelDB versioning blob. If not existed, return None
   */
  def get(path: String)(implicit ec: ExecutionContext): Option[Blob] = {
    load_blobs()
    blobs.get(path).map(versioningBlobToBlob)
  }

  /** Adds blob to this commit at path
   *  If path is already in this Commit, it will be updated to the new blob
   *  @param path Location to add blob to
   *  @param blob Instance of Blob subclass.
   */
  def update[T <: Blob](path: String, blob: T)(implicit ec: ExecutionContext) = {
    load_blobs()
    becomeChild()
    blobs.put(path, blob.versioningBlob)
  }

  /** Deletes the blob at path from this commit and return it
   *  @param path Location of the blob to removed
   *  @return the blob (if existed at path), or None
   */
  def remove(path: String)(implicit ec: ExecutionContext): Option[Blob] = {
    load_blobs()
    becomeChild()
    blobs.remove(path).map(versioningBlobToBlob)
  }

  /** Saves this commit to ModelDB
   *  @param message description of this commit
   *  TODO: write tests for this method
   */
  def save(message: String)(implicit ec: ExecutionContext) = {
    if (saved)
      Failure(new IllegalArgumentException("Commit is already saved"))
    else
      createCommit(message = message, blobs = Some(blobsList))
  }

  /** Return ancestors, starting from this Commit until the root of the Repository
   *  @return a list of ancestors
   */
  def log()(implicit ec: ExecutionContext): Try[List[Commit]] = {
    clientSet.versioningService.ListCommitsLog4(
      repository_id_repo_id = repo.id.get,
      commit_sha = commit.commit_sha.get
    ) // Try[VersioningListCommitsLogRequestResponse]
    .map(_.commits) // Try[Option[List[VersioningCommit]]]
    .map(ls => if (ls.isEmpty) null else ls.get.map(c => new Commit(clientSet, repo, c))) // Try[List[Commit]]
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
    )
  }

  /** Generates folder names and blob names in this commit by walking through its folder tree.
   *  TODO: handle the case when the commit is not saved
   *  TODO: modify the WalkOutput so that the folders are mutable
   */
  def walk()(implicit ec: ExecutionContext) = generateWalk(List(List()))

  private def generateWalk(locations: List[List[String]])(implicit ec: ExecutionContext): Stream[Try[WalkOutput]] = {
    if (locations.isEmpty) Stream()
    else {
      var location = locations.head
      var newLocations = locations.drop(1)

      val response = clientSet.versioningService.GetCommitComponent2(
        commit_sha = commit.commit_sha.get,
        repository_id_repo_id = repo.id.get,
        location = if (location.length > 0) Some(location) else None
      )

      val next = response match {
        case Failure(e) => Failure(e)
        case _ => {
          val folderPath = location.mkString("/")
          val responseFolder = response.get.folder

          val folderNames = responseFolder
          .flatMap(_.sub_folders) // Option[List[VersioningFolderElement]]
          .map(_.map(folder => folder.element_name.get))
          // Option[List[String]]
          .map(_.sortWith((name1: String, name2: String) => name1 < name2))

          val blobNames = responseFolder
          .flatMap(_.blobs) // Option[List[VersioningFolderElement]]
          .map(_.map(folder => folder.element_name.get))
          // Option[List[String]]
          .map(_.sortWith((name1: String, name2: String) => name1 < name2))

          // Extend locations to contain new locations:
          newLocations = folderNames
          .map(_.map(folder => location ::: List(folder))) // inefficient
          // Option[List[List[String]]]
          // push new location to stack:
          .getOrElse(Nil) ::: newLocations

          Success(new WalkOutput(folderPath, folderNames, blobNames))
        }
      }

      next #:: generateWalk(newLocations)
    }
  }

  /** Creates a branch at this Commit and returns the checked-out branch
   *  If the branch already exists, it will be moved to this commit.
   *  @param branch branch name
   *  @return if not saved, a failure; otherwise, this commit as the head of `branch`
   */
  def newBranch(branch: String)(implicit ec: ExecutionContext) = Try (
    if (!saved)
      throw new IllegalStateException("Commit must be saved before it can be attached to a branch")
    else {
      setBranch(branch)
      this
    }
  )

  /** Returns the diff from reference to self
   *  @param reference Commit to be compared to. If not provided, first parent will be used.
   *  @return Failure if this commit or reference has not yet been saved, or if they do not belong to the same repository; otherwise diff object.
   */
  def diffFrom(reference: Option[Commit] = None)(implicit ec: ExecutionContext) = {
    if (!saved)
      Failure(new IllegalStateException("Commit must be saved before a diff can be calculated"))
    else if (reference.isDefined && !reference.get.saved)
      Failure(new IllegalStateException("Reference must be saved before a diff can be calculated"))
    else if (reference.isDefined && reference.get.repo.id.get != repo.id.get)
      Failure(new IllegalStateException("Reference and this commit must belong to the same repository"))
    else {
      clientSet.versioningService.ComputeRepositoryDiff2(
        repository_id_repo_id = repo.id.get,
        commit_a = Some(
          if (reference.isDefined) reference.get.commit.commit_sha.get else commit.parent_shas.get.head
        ),
        commit_b = Some(commit.commit_sha.get)
      ).map(r => new Diff(r.diffs))
    }
  }

  /** Applies a diff to this Commit.
   *  This method creates a new commit in ModelDB, and assigns a new ID to this object.
   */
  def applyDiff(diff: Diff, message: String)(implicit ec: ExecutionContext) = {
    if (!saved)
      Failure(new IllegalStateException("Commit must be saved before a diff can be applied"))
    else {
      becomeChild()
      createCommit(message = message, diffs = diff.blobDiffs, commitBase = Some(commit.parent_shas.get.head))
    }
  }

  /** Reverts all the commits beginning with other up through this Commit
   *  This method creates and returns a new Commit in ModelDB, and assigns a new ID to this object
   *  @param other Base for the revert. If not provided, this commit will be reverted
   *  @param message Description of the revert. If not provided, a default message will be used
   *  @return Failure if this commit or other has not yet been saved, or if they do not belong to the same Repository; success otherwise.
   */
  def revert(other: Option[Commit] = None, message: Option[String] = None)(implicit ec: ExecutionContext) = Try(
    if (!saved)
      throw new IllegalStateException("This commit must be saved")
    else if (other.isDefined && !other.get.saved)
      throw new IllegalStateException("Other commit must be saved")
    else if (other.isDefined && other.get.repo.id.get != repo.id.get)
      throw new IllegalStateException("Two commits must belong to the same repository")
    else {
      clientSet.versioningService.RevertRepositoryCommits2(
        body = VersioningRevertRepositoryCommitsRequest(
          base_commit_sha = Some(commit.commit_sha.get),
          commit_to_revert_sha = Some(if (other.isDefined) other.get.commit.commit_sha.get
          else commit.commit_sha.get),
          content = Some(VersioningCommit(message=message))
        ),
        commit_to_revert_sha = if (other.isDefined) other.get.commit.commit_sha.get
        else commit.commit_sha.get,
        repository_id_repo_id = repo.id.get
      ).map(r => if (r.commit.isDefined) {
        commit = r.commit.get
        commit_branch.map(setBranch(_))
        init()
      })

      this
    }
  )

  /** Merges a branch headed by other into this commit
   *  This method creates and returns a new Commit in ModelDB, and assigns a new ID to this object
   *  @param other Commit to be merged
   *  @param message Description of the merge. If not provided, a default message will be used
   *  @return Failure if this commit or other has not yet been saved, or if they do not belong to the same Repository; success otherwise.
   *  TODO: is dry run?
   */
  def merge(other: Commit, message: Option[String] = None)(implicit ec: ExecutionContext) = Try(
    if (!saved)
      throw new IllegalStateException("This commit must be saved")
    else if (!other.saved)
      throw new IllegalStateException("Other commit must be saved")
    else if (other.repo.id.get != repo.id.get)
      throw new IllegalStateException("Two commits must belong to the same repository")
    else {
      val queryAttempt = clientSet.versioningService.MergeRepositoryCommits2(
        body = VersioningMergeRepositoryCommitsRequest(
          commit_sha_a = other.commit.commit_sha,
          commit_sha_b = commit.commit_sha,
          content = Some(VersioningCommit(message=message))
        ),
        repository_id_repo_id = repo.id.get
      ).map(r =>
      if (r.conflicts.isDefined) throw new IllegalStateException(
        List(
          "Merge conflict.", "Resolution is not currently supported through the client",
          "Please create a new Commit with the updated blobs.",
          "See https://docs.verta.ai/en/master/examples/tutorials/merge.html for instructions"
        ).mkString("\n")
      )
      else if (r.commit.isDefined) {
        commit = r.commit.get
        commit_branch.map(setBranch(_))
        init()
      })

      queryAttempt match {
        case Failure(e) => throw e
        case _ => this
      }
    }
  )

  /*** HELPER METHODS ***/

  /** Become child of current commit (if current commit is saved)
   */
  private def becomeChild() = {
    if (saved) {
      commit = VersioningCommit(
        parent_shas = commit.commit_sha.map(List(_))
      )
      saved = false
    }
  }

  /** Helper function to create a new commit and assign to current instance.
   *  TODO: better error handling for this
   */
  private def createCommit(message: String, blobs: Option[List[VersioningBlobExpanded]] = None,
    commitBase: Option[String] = None,
    diffs: Option[List[VersioningBlobDiff]] = None)(implicit ec: ExecutionContext) = {
      clientSet.versioningService.CreateCommit2(
        body = VersioningCreateCommitRequest(
          commit = Some(addMessage(message)),
          blobs = blobs,
          commit_base = commitBase,
          diffs = diffs
        ),
        repository_id_repo_id = repo.id.get
      )
      .map(r => if (!r.commit.isEmpty) {
        commit = r.commit.get
        commit_branch.map(setBranch(_))
        init()
      })
  }

  /** Convert a location to "repeated string" representation
      Based on Python's implementation
      @param path path
      @return the repeated string representation of the path
   */
   private def pathToLocation(path: String): List[String] = {
     if (path.charAt(0) == '/') pathToLocation(path.substring(1))
     else path.split("/").toList
   }

   /** Convert the dictionary of blobs into list form for API requests
    *  @return the list required
    */
  private def blobsList: List[VersioningBlobExpanded] = {
    (for ((path, blob) <- blobs) yield VersioningBlobExpanded(
        blob = Some(blob),
        location = Some(pathToLocation(path))
    )).toList
  }

  /** Add message to current commit. Done before saving
   *  @param message message
   */
  private def addMessage(message: String) = VersioningCommit(
    author = commit.author,
    commit_sha = commit.commit_sha,
    message = Some(message),
    parent_shas = commit.parent_shas
  )

  /** Reset the state of commit
   */
  private def init() = {
    saved = true
    loaded_from_remote = false
    blobs = new HashMap[String, VersioningBlob]()
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
      case Success(_) => {commit_branch = Some(branch)}
      case Failure(_) => {}
    }
  }
}


/** A class to represent the output of Commit's walk method
 *  @param folderPath path to current folder
 *  @param folderNames names of subfolders in current folder
 *  @param blobNames names of blobs in current folder
*/
class WalkOutput(val folderPath: String, val folderNames: Option[List[String]], val blobNames: Option[List[String]]) {}
