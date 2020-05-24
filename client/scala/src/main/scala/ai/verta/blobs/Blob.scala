package ai.verta.blobs

import ai.verta.swagger._public.modeldb.versioning.model._

trait Blob {
  val versioningBlob: VersioningBlob
}

trait Code extends Blob

trait Configuration extends Blob
