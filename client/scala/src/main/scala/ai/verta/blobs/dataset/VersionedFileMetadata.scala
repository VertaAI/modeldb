package ai.verta.blobs.dataset

/** Represent a file's metadata with (optionally) its versionID in S3 Blob
 *  @param metadata metadata of the file
 *  @param versionId (optional) version ID of the file.
 */
class VersionedFileMetadata(val metadata: FileMetadata, val versionId: Option[String] = None) {}
