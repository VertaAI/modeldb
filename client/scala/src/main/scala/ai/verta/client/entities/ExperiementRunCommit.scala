package ai.verta.client.entities

import ai.verta.client.Client
import ai.verta.repository.Commit

import scala.concurrent.ExecutionContext
import scala.util.{ Try, Success, Failure }

/**
 * Represent a commit associated with an experiment run.
 *  User should not initialize this. Instances of this class are returned from ExperimentRun's getCommit
 *  @param commitSHA commit SHA of the commit
 *  @param repoId the id of the commit's repository
 *  @param keyPaths A mapping between descriptive keys and paths of particular interest within commit.
 */
case class ExperimentRunCommit(
  val commitSHA: String,
  val repoId: BigInt,
  val keyPaths: Option[Map[String, String]]) {
  /**
   * Retrieve the Commit instance, given the client
   *  @param client client
   *  @return Commit instance corresponding to this ExperimentRunCommit instance, if succeeds
   */
  def toCommit(client: Client)(implicit ec: ExecutionContext): Try[Commit] =
    client.getRepository(repoId).flatMap(_.getCommitById(commitSHA))
}
