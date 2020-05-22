package ai.verta._repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model.VersioningRepository

class Repository(val clientSet: ClientSet, val repo: VersioningRepository) {
  // TODO: implement get commit
}
