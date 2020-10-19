package ai.verta.repository

import ai.verta.blobs.Blob

case class FilterWalker(root: String = "") extends FolderWalker[Option[String]] {
  override def filterFolder(folder: Folder): Folder = folder.filterSubfolders(name => !name.contains("b"))

  override def replace(folder: Folder): FolderWalker[Option[String]] = FilterWalker(folder.root)

  override def visitBlob(name: String, blob: Blob): Option[String] =
    if (root.length > 0) Some(root + "/" + name)
    else Some(name)
  override def visitFolder(name: String, folder: Folder): Option[String] = None
}
