package ai.verta.repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model.VersioningRepository

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/** ModelDB Repository.
 *  There should not be a need to instantiate this class directly; please use Client's getOrCreateRepository
 */
class Repository(private val clientSet: ClientSet, private val repo: VersioningRepository) {
  /** Get commit by its SHA id
   * @param id SHA ID of the commit
   * @return specified commit
   */
  def getCommitById(id: String)(implicit ec: ExecutionContext): Try[Commit] = {
    clientSet.versioningService.VersioningService_GetCommit2(
      repository_id_repo_id = repo.id.get,
      commit_sha = id
    ).map(r => new Commit(clientSet, this, r.commit.get))
  }

  /** Get commit by specified branch
   *  @param branch branch of commit. If not passed, then use "master" branch
   *  @return specified commit
   */
   def getCommitByBranch(branch: String = "master")(implicit ec: ExecutionContext): Try[Commit] = {
     clientSet.versioningService.VersioningService_GetBranch2(
       branch = branch,
       repository_id_repo_id = repo.id.get
     ).map(r => new Commit(clientSet, this, r.commit.get, Some(branch)))
   }

   /** Get commit by specified tag
    *  @param tag tag of commit.
    *  @return specified commit
    */
   def getCommitByTag(tag: String)(implicit ec: ExecutionContext): Try[Commit] = {
     clientSet.versioningService.VersioningService_GetTag2(
       tag = tag,
       repository_id_repo_id = repo.id.get
     ).map(r => new Commit(clientSet, this, r.commit.get))
   }

   /** Delete a tag from this repository
    *  @param tag tag
    */
    def deleteTag(tag: String)(implicit ec: ExecutionContext): Try[Unit] = {
      clientSet.versioningService.VersioningService_DeleteTag2(
          repository_id_repo_id = repo.id.get,
          tag = tag
      ).map(_ => ())
    }


    override def equals(other: Any) = other match {
      case other: Repository => repo.id.get == other.repo.id.get
      case _ => false
    }

    /** Return the name of the repository */
    def name = repo.name.get

    /** Return the id of the repository */
    def id = repo.id.get
}
