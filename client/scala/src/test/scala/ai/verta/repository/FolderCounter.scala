package ai.verta.repository

import ai.verta.blobs.Blob

case class FolderCounter() extends FolderWalker[Int] {
  override def visitBlob(name: String, blob: Blob): Int = 0
  override def visitFolder(name: String, folder: Folder): Int = 1
}
