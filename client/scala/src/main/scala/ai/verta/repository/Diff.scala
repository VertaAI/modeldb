package ai.verta.repository

import ai.verta.swagger._public.modeldb.versioning.model._

class Diff(val blobDiffs: Option[List[VersioningBlobDiff]]) {}
