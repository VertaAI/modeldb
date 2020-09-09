package ai.verta.dataset_versioning

import ai.verta.blobs.dataset.PathBlob
import ai.verta.swagger._public.modeldb.model._
import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.swagger.client.ClientSet

/** Represents a ModelDB dataset version from path.
 */
class DatasetVersion(
  private val clientSet: ClientSet,
  private val dataset: Dataset,
  private val datasetVersion: ModeldbDatasetVersion
) {
  /** ID of the dataset version. */
  def id = datasetVersion.id.get
}
