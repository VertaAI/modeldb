package ai.verta.dataset_versioning

import ai.verta.swagger._public.modeldb.model._
import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.swagger.client.ClientSet

class Dataset(private val clientSet: ClientSet, private val dataset: ModeldbDataset) {
  def id: String = dataset.id.get
  def name: String = dataset.name.get
}
