package ai.verta._repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model.VersioningRepository

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}


/** ModelDB Repository
 *  There should not be a need to instantiate this class directly; please use Client's getOrCreateRepository
 */
class Repository(val clientSet: ClientSet, val repo: VersioningRepository) {
  // TODO: implement get commit

  /** Get commit by commit sha
  * @param id sha id of the commit
  */
  // def getCommitById(val id: String)(implicit ec: ExecutionContext) {
  //   clientSet.versioningService.GetCommit2(
  //     repository_id_repo_id = getId(),
  //     commit_sha = id,
  //     repository_id_named_id_name = "", // dummy value
  //     repository_id_named_id_workspace_name = "" // dummy value
  //   )
  //   .map(r => if r.commit.isEmpty null else new Commit(clientSet, r.commit.get))
  // }

  /** Get the id of repository
   */
  private def getId(): BigInt = {
    repo.id match {
      case Some(v) => v
      case _ => null
    }
  }
}
