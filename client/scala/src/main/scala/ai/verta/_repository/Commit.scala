package ai.verta._repository

import ai.verta.client.getPersonalWorkspace
import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model.{VersioningCommit, VersioningRepository}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/** Commit within a ModelDB Repository
 * There should not be a need to instantiate this class directly; please use Repository.getCommit methods
 */
class Commit(val clientSet: ClientSet, val repo: VersioningRepository, val commit: VersioningCommit) {

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
}
