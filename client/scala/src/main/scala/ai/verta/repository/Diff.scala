package ai.verta.repository

import ai.verta.swagger._public.modeldb.versioning.model._

/** Represent the diff between two commits */
class Diff(val blobDiffs: Option[List[VersioningBlobDiff]]) {}
