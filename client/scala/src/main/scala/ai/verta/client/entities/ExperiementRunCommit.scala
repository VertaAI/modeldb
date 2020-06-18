package ai.verta.client.entities

import ai.verta.client.Client
import ai.verta.repository.Commit

import scala.concurrent.ExecutionContext
import scala.util.{ Try, Success, Failure }

/** Represent a commit associated with an experiment run.
 *  User should not initialize this. Instances of this class are returned from ExperimentRun's getCommit
 *  @param commit commit instance
 *  @param keyPaths A mapping between descriptive keys and paths of particular interest within commit.
 */
case class ExperimentRunCommit(val commit: Commit, val keyPaths: Option[Map[String, String]]) {}
