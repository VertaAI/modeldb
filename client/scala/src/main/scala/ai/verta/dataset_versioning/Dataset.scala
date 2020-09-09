package ai.verta.dataset_versioning

import ai.verta.swagger._public.modeldb.model._
import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.swagger.client.ClientSet

/** Represents a ModelDB dataset.
 */
class Dataset(private val clientSet: ClientSet, private val dataset: ModeldbDataset) {
  /** ID of the dataset. */
  def id: String = dataset.id.get

  /** Name of the dataset. */
  def name: String = dataset.name.get
}
