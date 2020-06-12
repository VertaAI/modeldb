package ai.verta.repository

import ai.verta.swagger._public.modeldb.versioning.model._

/** Represent the diff between two commits. This should not be instantiated by user.
 *  Please use Commit's diffFrom method to get instances
 */
class Diff(private[repository] val blobDiffs: Option[List[VersioningBlobDiff]]) {}
