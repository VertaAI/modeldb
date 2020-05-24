package ai.verta.blobs

import ai.verta.swagger._public.modeldb.versioning.model._

/** Captures metadata about the Jupyter Notebook at path and the current git environment
 *  TODO: implement autocapture behaviour
 */
case class Notebook(val path: String, val git: Option[Git] = None) extends Code {
  // Basically a wrapper for VersioningNotebookCodeBlob

  val versioningNotebookCodeBlob = VersioningNotebookCodeBlob(
    /* TODO: expanduser path (see python client's code) */
    path = Some(VersioningPathDatasetComponentBlob(path = Some(path))),
    git_repo = git.map(_.versioningGitCodeBlob)
  )

  val versioningBlob = VersioningBlob(
    code = Some(VersioningCodeBlob(notebook = Some(versioningNotebookCodeBlob)))
  )
}
