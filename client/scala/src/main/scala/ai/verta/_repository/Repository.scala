package ai.verta._repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model.VersioningRepository
import ai.verta.client.getPersonalWorkspace

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}


/** ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Client's getOrCreateRepository
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
