package ai.verta._repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model.VersioningRepository
import ai.verta.client.{getPersonalWorkspace, urlEncode}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}


/** ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Client's getOrCreateRepository
 *  TODO: refactor get commit methods using higher order function
 */
class Repository(val clientSet: ClientSet, val repo: VersioningRepository) {
  /** Get commit by its SHA id
   * @param id SHA ID of the commit
   * @return specified commit
   */
  def getCommitById(id: String)(implicit ec: ExecutionContext): Try[Commit] = {
    clientSet.versioningService.GetCommit2(
      repository_id_repo_id = getId(),
      commit_sha = id
    )
    .map(r => if (r.commit.isEmpty) null else new Commit(clientSet, repo, r.commit.get))
  }

  /** Get commit by specified branch
   *  @param branch branch of commit. If not passed, then use "master" branch
   *  @return specified commit
   */
   def getCommitByBranch(branch: String = "master")(implicit ec: ExecutionContext): Try[Commit] = {
     clientSet.versioningService.GetBranch2(
       branch = branch,
       repository_id_repo_id = getId()
     )
     .map(r => if (r.commit.isEmpty) null else new Commit(clientSet, repo, r.commit.get, Some(branch)))
   }

   /** Get commit by specified tag
    *  @param tag tag of commit.
    *  @return specified commit
    */
   def getCommitByTag(tag: String)(implicit ec: ExecutionContext): Try[Commit] = {
     clientSet.versioningService.GetTag2(
       tag = urlEncode(tag),
       repository_id_repo_id = getId()
     )
     .map(r => if (r.commit.isEmpty) null else new Commit(clientSet, repo, r.commit.get))
   }

   /** Delete a tag from this repository
    *  @param tag tag
    */
    def deleteTag(tag: String)(implicit ec: ExecutionContext) = {
      clientSet.versioningService.DeleteTag2(
          repository_id_repo_id = repo.id.get,
          tag = urlEncode(tag)
      )
    }

  /** Get the id of repository
   */
  private def getId(): BigInt = {
    repo.id match {
      case Some(v) => v
      case _ => null
    }
  }

  /** Get the name of repository
   */
  private def getName(): String = {
    repo.name match {
      case Some(v) => v
      case _ => null
    }
  }
}
