package ai.verta._repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model.VersioningCommit

class Commit(val clientSet: ClientSet, val commit: VersioningCommit) {

}
