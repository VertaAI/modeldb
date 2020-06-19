package ai.verta.repository

import ai.verta.blobs.Blob

case class BlobCounter() extends FolderWalker[Int] {
  override def visitBlob(name: String, blob: Blob): Int = 1

  override def visitFolder(name: String, folder: Folder): Int = 0
}
