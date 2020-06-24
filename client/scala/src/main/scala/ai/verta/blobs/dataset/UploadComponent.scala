package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._

/** The versioning path dataset component to be uploaded, along with the path to the file in the local file system */
case class UploadComponent(val filePath: String, val component: VersioningPathDatasetComponentBlob)
