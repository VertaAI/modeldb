package ai.verta._repository

import ai.verta.swagger.client.ClientSet
import ai.verta.swagger._public.modeldb.versioning.model.VersioningRepository

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class Repository(val clientSet: ClientSet, val repo: VersioningRepository) {
  // TODO: implement get commit

  // def getCommitById(id)(implicit ec: ExecutionContext) {
  //   val repo_id = getId()
  // }

  private def getId(): BigInt = {
    repo.id match {
      case Some(v) => v
      case _ => null
    }
  }
}
