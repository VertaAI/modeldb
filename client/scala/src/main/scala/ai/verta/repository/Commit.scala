package ai.verta.repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.collection.immutable.Map
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

import java.io.{File, FileInputStream, FileOutputStream, ByteArrayInputStream}
import java.nio.file.{Files, StandardCopyOption}

/** Commit within a ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Repository.getCommit methods
 */
class Commit(
  private val clientSet: ClientSet, private val repo: Repository,
  private val commit: VersioningCommit, private val commitBranch: Option[String] = None
) {
  private var loadedFromRemote = false // whether blobs has been retrieved from remote
  private var blobs = Map[String, Blob]() // mutable map for storing blobs. Only loaded when used

  /** Return the id of the commit */
  def id = commit.commit_sha

  /** Return the id of the commit's repository */
  def repoId = repo.id

  /** Whether the commit instance is saved to database, or is currently being modified.
   *  A commit is saved if and only if its versioning commit field has a defined ID.
   */
  private def saved = id.isDefined

  override def equals(other: Any) = other match {
    case other: Commit => saved && other.saved && id.get == other.id.get
    case _ => false
  }

  /** Retrieves the blob at path from this commit
   *  @param path location of a blob
   *  @return The blob. If not existed, or retrieving blobs from backend fails, return a failure.
   */
  def get(path: String)(implicit ec: ExecutionContext): Try[Blob] =
    loadBlobs().flatMap(_ =>
      blobs.get(path) match {
        case None => Failure(new NoSuchElementException("No blob was stored at this path."))
        case Some(blob) => toMDBVersioningDataset(blob).fold(Success(blob))(dataBlob => {
          /** TODO: remove this mutation */
          dataBlob.commit = Some(this)
          dataBlob.blobPath = Some(path)
          Success(dataBlob)
        })
      }
    )

  /** Adds blob to this commit at path
   *  If path is already in this Commit, it will be updated to the new blob
   *  @param path Location to add blob to
   *  @param blob Instance of Blob subclass.
   *  @return The new commit, if succeeds.
   */
  def update(path: String, blob: Blob)(implicit ec: ExecutionContext): Try[Commit] = {
    loadBlobs().map(_ => getChild(blobs + (path -> blob)))
  }

  /** Remove a blob to this commit at path
   *  @param path Location of the blob to removed.
   *  @return The new commit with the blob removed, if succeeds.
   */
  def remove(path: String)(implicit ec: ExecutionContext) =
    get(path).map(_ => getChild(blobs - path))

  /** Saves this commit to ModelDB
   *  @param message description of this commit
   *  @return The saved commit, if succeeds.
   */
  def save(message: String)(implicit ec: ExecutionContext) = {
    if (saved)
      Failure(new IllegalCommitSavedStateException("Commit is already saved"))
    else
      loadBlobs().flatMap(_ => {
        val blobsToVersion = blobs
          .mapValues(toMDBVersioningDataset)
          .filter(pair => pair._2.isDefined)
          .mapValues(_.get) // Map[String, Dataset]
          .filter(pair => pair._2.enableMDBVersioning)

        // trigger preparing for upload
        val newCommit = Try(blobsToVersion.values.foreach(dataset => dataset.prepareForUpload().get))
          .flatMap(_ => blobsList())
          .flatMap(list => createCommit(message = message, blobs = Some(list), updateBranch = false))
        // do not update the branch's head right away (in case uploading data fails)

        // upload the artifacts given by blobsToVersion map then clean up
        val uploadAttempt: Try[Unit] = newCommit.map(newCommit => {
          blobsToVersion
            .mapValues(_.getAllMetadata) // Map[String, Iterable[FileMetadata]]
            .map(pair => pair._2.map(metadata => newCommit.uploadArtifact(pair._1, metadata.path, new File(metadata.localPath.get))))
        }).flatMap(_ => Try(blobsToVersion.values.map(_.cleanUpUploadedComponents()).map(_.get)))

        uploadAttempt.flatMap(_ =>
          if (commitBranch.isDefined)
            newCommit.flatMap(_.newBranch(commitBranch.get))
          else
            newCommit
        ) // if uploading fails, return the failure instead
      })
  }

  /** Helper function to create a new commit and assign to current instance.
   *  @param message the message assigned to the new commit
   *  @param blobs The list of blobs to assign to the new commit (optional)
   *  @param commitBase base for the new commit (optional)
   *  @param diffs a list of diffs (optional)
   *  @param updateBranch whether to set the new commit to head of the current commit's branch. Default is true
   *  @return the new commit (if succeeds), set to the current branch's head (if there is a branch)
   */
  private def createCommit(
    message: String,
    blobs: Option[List[VersioningBlobExpanded]] = None,
    commitBase: Option[String] = None,
    diffs: Option[List[VersioningBlobDiff]] = None,
    updateBranch: Boolean = true
  )(implicit ec: ExecutionContext) = {
      clientSet.versioningService.CreateCommit2(
        body = VersioningCreateCommitRequest(
          commit = Some(addMessage(message)),
          blobs = blobs,
          commit_base = commitBase,
          diffs = diffs
        ),
        repository_id_repo_id = repo.id
      ).flatMap(r => versioningCommitToCommit(r.commit.get, updateBranch))
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
        blob = Some(blobToVersioningBlob(blob)),
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
  private def loadBlobs()(implicit ec: ExecutionContext): Try[Unit] = this.synchronized {
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
  )(implicit ec: ExecutionContext): Try[Map[String, Blob]] = {
    clientSet.versioningService.ListCommitBlobs2(
      commit_sha = id,
      repository_id_repo_id = repo.id
    ) // Try[VersioningListCommitBlobsRequestResponse]
    .map(_.blobs) // Try[Option[List[VersioningBlobExpanded]]]
    .map(ls =>
      if (ls.isEmpty) Map[String, Blob]()
      else ls.get.map(blob => blob.location.get.mkString("/") -> versioningBlobToBlob(blob.blob.get)).toMap
    )
  }

  /** Return a child commit child of current commit (if current commit is saved) with the given blobs
   *  This helper function is used for modifcation
   *  @param childBlobs the blobs of the child commit
   *  @return the child commit instance, if loading blobs succeeds.
   */
  private def getChild(childBlobs: Map[String, Blob])(implicit ec: ExecutionContext) = {
      /** TODO: Deal with author, date_created */
      val newVersioningCommit = VersioningCommit(
        parent_shas = if (saved) commit.commit_sha.map(List(_)) else commit.parent_shas
      )

      val child = new Commit(clientSet, repo, newVersioningCommit, commitBranch)
      child.blobs = childBlobs
      child.loadedFromRemote = true  // child's blobs are already provided

      child
    }

  /** Helper function to convert a VersioningBlob instance to corresponding Blob subclass instance
   *  @param vb the VersioningBlob instance
   *  @return an instance of a Blob subclass
   */
  private def versioningBlobToBlob(vb: VersioningBlob): Blob = vb match {
    /** TODO: finish the pattern matching with other blob subclasses */
    case VersioningBlob(_, _, Some(VersioningDatasetBlob(Some(path), _)), _) => PathBlob(path)
    case VersioningBlob(_, _, Some(VersioningDatasetBlob(_, Some(s3))), _) => S3(s3)
  }

  private def blobToVersioningBlob(blob: Blob): VersioningBlob =  blob match {
    /** TODO: Add blob subtypes to pattern matching */
    case pathBlob: PathBlob => PathBlob.toVersioningBlob(pathBlob)
    case s3: S3 => S3.toVersioningBlob(s3)
  }


  /** Creates a branch at this Commit and returns the checked-out branch
   *  If the branch already exists, it will be moved to this commit.
   *  @param branch branch name
   *  @return if not saved, a failure; otherwise, this commit as the head of `branch`
   */
  def newBranch(branch: String)(implicit ec: ExecutionContext) = {
    checkSaved("Commit must be saved before it can be attached to a branch").flatMap(_ =>
      clientSet.versioningService.SetBranch2(
        body = commit.commit_sha.get,
        branch = branch,
        repository_id_repo_id = repo.id
      ).flatMap(_ => repo.getCommitByBranch(branch))
    )
  }

  /** Assigns a tag to this Commit
   *  @param tag tag
   */
  def tag(tag: String)(implicit ec: ExecutionContext) = {
    checkSaved("Commit must be saved before it can be tagged").flatMap(_ =>
      clientSet.versioningService.SetTag2(
          body = commit.commit_sha.get,
          repository_id_repo_id = repo.id,
          tag = tag
      )
    ).map(_ => ())
  }

  /** Merges a branch headed by other into this commit
   *  This method creates and returns a new Commit in ModelDB, and assigns a new ID to this object
   *  @param other Commit to be merged
   *  @param message Description of the merge. If not provided, a default message will be used
   *  @return Failure if this commit or other has not yet been saved, or if they do not belong to the same Repository; the merged commit otherwise.
   */
  def merge(other: Commit, message: Option[String] = None)(implicit ec: ExecutionContext) = {
    checkSaved("This commit must be saved").flatMap(_ => other.checkSaved("Other commit must be saved"))
      .flatMap(_ => checkSameRepository(other))
      .flatMap(_ => {
        clientSet.versioningService.MergeRepositoryCommits2(
          /** TODO: is dry run? */
          body = VersioningMergeRepositoryCommitsRequest(
            commit_sha_a = other.id,
            commit_sha_b = id,
            content = Some(VersioningCommit(message=message))
          ),
          repository_id_repo_id = repo.id
        ).flatMap(r =>
        if (r.conflicts.isDefined) Failure(throw new IllegalArgumentException(
          List(
            "Merge conflict.", "Resolution is not currently supported through the client",
            "Please create a new Commit with the updated blobs.",
            "See https://docs.verta.ai/en/master/examples/tutorials/merge.html for instructions"
          ).mkString("\n")
        ))
        else versioningCommitToCommit(r.commit.get))
      }
    )
  }

  /** Helper function to convert the versioning commit instance to commit instance
   *  If the current instance has a branch associated with it, the new commit will become the head of the branch.
   *  Useful for createCommit, merge, and revert
   *  @param versioningCommit the versioning commit instance
   *  @param updateBranch whether to set the new commit to head of the current commit's branch. Default is true
   *  @return the corresponding commit instance
   */
  private def versioningCommitToCommit(
    versioningCommit: VersioningCommit,
    updateBranch: Boolean = true
  )(implicit ec: ExecutionContext) = {
    val newCommit = new Commit(clientSet, repo, versioningCommit, commitBranch)

    if (updateBranch && commitBranch.isDefined)
      newCommit.newBranch(commitBranch.get)
    else
      Success(newCommit)
  }

  /** Return ancestors, starting from this Commit until the root of the Repository
   *  @return a list of ancestors
   */
  def log()(implicit ec: ExecutionContext): Try[Stream[Commit]] = {
    // if the current commit is not saved (no sha), get the one of its parent
    // (the base of the modification)
    val commitSHA = commit.commit_sha.getOrElse(commit.parent_shas.get.head)

    clientSet.versioningService.ListCommitsLog4(
      repository_id_repo_id = repo.id,
      commit_sha = commitSHA
    ) // Try[VersioningListCommitsLogRequestResponse]
    .map(_.commits) // Try[Option[List[VersioningCommit]]]
    .map(ls =>
      if (ls.isEmpty) Stream()
      else ls.get.toStream.map(c => new Commit(clientSet, repo, c))
    )
  }

  /** Reverts other.
   *  This method creates and returns a new Commit in ModelDB, and assigns a new ID to this object
   *  Currently reverting a merge commit is not supported. Unexpected behavior might occur.
   *  @param other Base for the revert. If not provided, this commit will be reverted
   *  @param message Description of the revert. If not provided, a default message will be used
   *  @return The new commit, with the changes in other reverted, if suceeds. Failure if this commit or other has not yet been saved, or if they do not belong to the same Repository.
   */
  def revert(other: Commit = this, message: Option[String] = None)(implicit ec: ExecutionContext) = {
    checkSaved("This commit must be saved").flatMap(_ => other.checkSaved("Other commit must be saved"))
      .flatMap(_ => checkSameRepository(other))
      .flatMap(_ =>
        clientSet.versioningService.RevertRepositoryCommits2(
          body = VersioningRevertRepositoryCommitsRequest(
            base_commit_sha = Some(id.get),
            commit_to_revert_sha = Some(other.id.get),
            content = Some(VersioningCommit(message=message))
          ),
          commit_to_revert_sha = other.id.get,
          repository_id_repo_id = repo.id
        ).flatMap(r => versioningCommitToCommit(r.commit.get))
      )
  }

  /** Returns the diff from reference to self
   *  @param reference Commit to be compared to. If not provided, first parent will be used.
   *  @return Failure if this commit or reference has not yet been saved, or if they do not belong to the same repository; otherwise diff object.
   */
  def diffFrom(reference: Option[Commit] = None)(implicit ec: ExecutionContext) = {
    checkSaved("Commit must be saved before a diff can be calculated").flatMap(_ =>
      if (reference.isDefined)
        reference.get.checkSaved("Reference must be saved before a diff can be calculated")
                 .flatMap(_ => checkSameRepository(reference.get))
      else
        Success(())
    ).flatMap(_ => clientSet.versioningService.ComputeRepositoryDiff2(
        repository_id_repo_id = repo.id,
        commit_a = Some(
          if (reference.isDefined) reference.get.id.get else commit.parent_shas.get.head
        ),
        commit_b = Some(commit.commit_sha.get)
      ).map(r => new Diff(r.diffs))
    )
  }

  /** Applies a diff to this Commit.
   *  This method creates a new commit in ModelDB, and assigns a new ID to this object.
   *  @param diff the Diff instance returned by diffFrom
   *  @param message the message associated with the new commit
   *  @return the new Commit instance, if succeeds.
   */
  def applyDiff(diff: Diff, message: String)(implicit ec: ExecutionContext) = {
    checkSaved("Commit must be saved before a diff can be applied")
      .flatMap(_ => loadBlobs())
      .map(_ => getChild(blobs)) // new commit's parent is old commit
      .flatMap(_.createCommit(message = message, diffs = diff.blobDiffs, commitBase = id))
  }

  /** Check that the commit is saved.
   *  @param message error message if this commit is not saved
   *  @return Failure if the commit is not saved. Success otherwise
   */
  def checkSaved(message: String): Try[Unit] = {
    if (!saved)
      Failure(new IllegalCommitSavedStateException(message))
    else
      Success(())
  }

  /** Check that the other commit is in the same repository as this commit.
   *  @param message error message if the two commits are not in the same repository.
   *  @return Failure if the two commits are not in the same repository. Success otherwise
   */
  private def checkSameRepository(other: Commit): Try[Unit] = {
    if (repo != other.repo)
      Failure(new IllegalArgumentException("Two commits must belong to the same repository"))
    else
      Success(())
  }

  /** Generates a stream of outputs in this commit by walking through its folder tree.
   *  The stream ends at the first failure, or when there are no folders left. If the commit is not saved, its only element is that Failure
   *  @param walker the FolderWalker to process the folders in the folder tree.
   *  @return a stream of Try's of WalkOutput.
   */
  def walk[T](walker: FolderWalker[T])(implicit ec: ExecutionContext): Stream[Try[T]] = {
    if (!saved)
      Stream(Failure(new IllegalCommitSavedStateException("Commit must be saved before it can be walked")))
    else {
      getFolder(PathList(Nil)) match {
        case Failure(e) => Stream(Failure(e))
        case Success(root) => continueWalk(root, walker)
      }
    }
  }

  /** Get the folder corresponding to a path in list form
   *  @param location location
   *  @return the folder, if succeeds
   */
  def getFolder(location: PathList)(implicit ec: ExecutionContext): Try[Folder] = {
    clientSet.versioningService.GetCommitComponent2(
      commit_sha = id.get,
      repository_id_repo_id = repo.id,
      location = if (location.components.length > 0) Some(location.components) else None
    ).flatMap(r => {
        val folderPath = location.path
        val responseFolder = r.folder

        val folderNames = responseFolder.flatMap(_.sub_folders) // Option[List[VersioningFolderElement]]
                                        .map(_.map(folder => folder.element_name.get))
                                        // Option[List[String]]
                                        .map(_.sorted)

        val blobNames = responseFolder.flatMap(_.blobs) // Option[List[VersioningFolderElement]]
                                      .map(_.map(folder => folder.element_name.get))
                                      // Option[List[String]]
                                      .map(_.sorted)

        Success(Folder(folderPath, blobNames.getOrElse(Nil), folderNames.getOrElse(Nil)))
      })
  }

  /** Continue the walk from a folder.
   *  @param folder current folder being explored.
   *  @param walker a FolderWalker instance to process the returned folder
   *  @return Stream of Trys. If the returned WalkOutput fails, abort the remaining locations.
   */
  def continueWalk[T](folder: Folder, walker: FolderWalker[T])(implicit ec: ExecutionContext): Stream[Try[T]] = {
    // process the folder and walker:
    val replacedWalker = walker.replace(folder)
    val filteredFolder = replacedWalker.filterFolder(folder)

    val subfolders: Try[List[Folder]] =
      Try(filteredFolder.subfolderPaths.map(folder => getFolder(PathList(folder.split("/").toList))).map(_.get))
    val blobs: Try[List[Blob]] =
      Try(filteredFolder.blobPaths.map(get).map(_.get))

    subfolders match {
      case Failure(e) => Stream(Failure(e))
      case Success(sf) => {
        blobs match {
          case Failure(e) => Stream(Failure(e))
          case Success(bl) => {
            val blobResults: Stream[Try[T]] =
              filteredFolder.blobs.zip(bl).toStream.map(pair => Success(replacedWalker.visitBlob(pair._1, pair._2)))
            val subfolderResults: Stream[Try[T]] =
              filteredFolder.subfolders.zip(sf).toStream.map(pair => Success(replacedWalker.visitFolder(pair._1, pair._2)))

            blobResults #::: subfolderResults #::: sf.toStream.flatMap(folder => continueWalk(folder, replacedWalker))
          }
        }
      }
    }
  }

  /** Convert the blob to a dataset (if the blob is a dataset)
   *  @param blob the blob
   *  @return Some dataset, if the blob is a dataset; otherwise None
   */
  private def toMDBVersioningDataset(blob: Blob): Option[Dataset] = blob match {
    case PathBlob(contents, enableMDBVersioning) => Some(PathBlob(contents, enableMDBVersioning))
    case S3(contents, enableMDBVersioning) => Some(S3(contents, enableMDBVersioning))
    case _ => None
  }

  /** Helper method to retrieve URL to upload the file.
   *  @param blobPath path to the blob in the commit
   *  @param datasetComponentPath path to the component in the blob
   *  @param method either PUT or GET
   *  @return The URL, if succeeds
   */
  private def getURLForArtifact(
    blobPath: String,
    datasetComponentPath: String,
    method: String
  )(implicit ec: ExecutionContext): Try[String] = {
    clientSet.versioningService.getUrlForBlobVersioned2(
      VersioningGetUrlForBlobVersioned(
        commit_sha = id,
        location = Some(pathToLocation(blobPath)),
        method = Some(method),
        part_number = Some(BigInt(0)),
        path_dataset_component_blob_path = Some(datasetComponentPath),
        repository_id = Some(VersioningRepositoryIdentification(repo_id = Some(repoId)))
      ),
      id.get,
      repoId
    ).map(_.url.get)
  }

  /** Helper method to upload the file to ModelDB. Currently not supporting multi-part upload
   *  @param blobPath path to the blob in the commit
   *  @param datasetComponentPath path to the component in the blob
   *  @return whether the upload attempt succeeds
   */
  private def uploadArtifact(
    blobPath: String,
    datasetComponentPath: String,
    file: File
  )(implicit ec: ExecutionContext): Try[Unit] = {
    /** TODO: implement multi-part upload */
    getURLForArtifact(blobPath, datasetComponentPath, "PUT").flatMap(url =>
      Try (new FileInputStream(file)).flatMap(inputStream => { // loan pattern
        try {
          Await.result(clientSet.client.requestRaw("PUT", url, null, null, inputStream), Duration.Inf)
            .map(_ => ())
        } finally {
          inputStream.close()
        }
      })
    )
  }

  /** Helper method to download a component of a blob.
   *  @param blobPath path to the blob in the commit
   *  @param datasetComponentPath path to the component in the blob
   *  @param file File to write the downloaded content to
   *  @return whether the download attempt succeeds
   */
  private[verta] def downloadComponent(
    blobPath: String,
    datasetComponentPath: String,
    file: File
  )(implicit ec: ExecutionContext): Try[Unit] = {
    getURLForArtifact(blobPath, datasetComponentPath, "GET").flatMap(url =>
      Await.result(
        clientSet.client.requestRaw("GET", url, null, null, null)
          .map(resp => resp match {
            case Success(response) => Try(new ByteArrayInputStream(response)).flatMap(inputStream => {
              try {
                Try(Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING))
              }
              finally {
                inputStream.close()
              }
            })
            case Failure(e) => Failure(e)
          }),
        Duration.Inf)
    )
  }
}
