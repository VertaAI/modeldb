package ai.verta.repository

import ai.verta.blobs.Blob

case class CommitLister(root: String = "") extends FolderWalker[Option[String]] {
  override def replace(folder: Folder): FolderWalker[Option[String]] = CommitLister(folder.root)
  override def visitBlob(name: String, blob: Blob): Option[String] =
    if (root.length > 0) Some(root + "/" + name)
    else Some(name)
  override def visitFolder(name: String, folder: Folder): Option[String] = None
}
