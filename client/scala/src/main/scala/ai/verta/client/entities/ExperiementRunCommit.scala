package ai.verta.client.entities

/** An experiement run's commit SHA, its repository's id, and the associated key-path map of the experiment run.
 *  User should not initialize this. Instances of this class are returned from ExperimentRun's getCommit
 */
class ExperimentRunCommit(val commitSHA: String, val repoId: BigInt, val keyPaths: Option[Map[String, String]]) {}
