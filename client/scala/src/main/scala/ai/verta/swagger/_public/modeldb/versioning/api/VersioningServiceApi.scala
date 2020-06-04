// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.versioning.model._

class VersioningServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def ComputeRepositoryDiffAsync(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, branch_a: Option[String]=None, branch_b: Option[String]=None, commit_a: Option[String]=None, commit_b: Option[String]=None, replace_a_with_common_ancestor: Option[Boolean]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningComputeRepositoryDiffRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (commit_a.isDefined) __query.update("commit_a", client.toQuery(commit_a.get))
    if (commit_b.isDefined) __query.update("commit_b", client.toQuery(commit_b.get))
    if (replace_a_with_common_ancestor.isDefined) __query.update("replace_a_with_common_ancestor", client.toQuery(replace_a_with_common_ancestor.get))
    if (branch_a.isDefined) __query.update("branch_a", client.toQuery(branch_a.get))
    if (branch_b.isDefined) __query.update("branch_b", client.toQuery(branch_b.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningComputeRepositoryDiffRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/diff", __query.toMap, body, VersioningComputeRepositoryDiffRequestResponse.fromJson)
  }

  def ComputeRepositoryDiff(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, branch_a: Option[String]=None, branch_b: Option[String]=None, commit_a: Option[String]=None, commit_b: Option[String]=None, replace_a_with_common_ancestor: Option[Boolean]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningComputeRepositoryDiffRequestResponse] = Await.result(ComputeRepositoryDiffAsync(repository_id_named_id_name, repository_id_named_id_workspace_name, branch_a, branch_b, commit_a, commit_b, replace_a_with_common_ancestor, repository_id_repo_id), Duration.Inf)

  def ComputeRepositoryDiff2Async(repository_id_repo_id: BigInt, branch_a: Option[String]=None, branch_b: Option[String]=None, commit_a: Option[String]=None, commit_b: Option[String]=None, replace_a_with_common_ancestor: Option[Boolean]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningComputeRepositoryDiffRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (commit_a.isDefined) __query.update("commit_a", client.toQuery(commit_a.get))
    if (commit_b.isDefined) __query.update("commit_b", client.toQuery(commit_b.get))
    if (replace_a_with_common_ancestor.isDefined) __query.update("replace_a_with_common_ancestor", client.toQuery(replace_a_with_common_ancestor.get))
    if (branch_a.isDefined) __query.update("branch_a", client.toQuery(branch_a.get))
    if (branch_b.isDefined) __query.update("branch_b", client.toQuery(branch_b.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningComputeRepositoryDiffRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/diff", __query.toMap, body, VersioningComputeRepositoryDiffRequestResponse.fromJson)
  }

  def ComputeRepositoryDiff2(repository_id_repo_id: BigInt, branch_a: Option[String]=None, branch_b: Option[String]=None, commit_a: Option[String]=None, commit_b: Option[String]=None, replace_a_with_common_ancestor: Option[Boolean]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningComputeRepositoryDiffRequestResponse] = Await.result(ComputeRepositoryDiff2Async(repository_id_repo_id, branch_a, branch_b, commit_a, commit_b, replace_a_with_common_ancestor, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def CreateCommitAsync(body: VersioningCreateCommitRequest, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningCreateCommitRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningCreateCommitRequest, VersioningCreateCommitRequestResponse]("POST", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits", __query.toMap, body, VersioningCreateCommitRequestResponse.fromJson)
  }

  def CreateCommit(body: VersioningCreateCommitRequest, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningCreateCommitRequestResponse] = Await.result(CreateCommitAsync(body, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def CreateCommit2Async(body: VersioningCreateCommitRequest, repository_id_repo_id: BigInt)(implicit ec: ExecutionContext): Future[Try[VersioningCreateCommitRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningCreateCommitRequest, VersioningCreateCommitRequestResponse]("POST", basePath + s"/versioning/repositories/$repository_id_repo_id/commits", __query.toMap, body, VersioningCreateCommitRequestResponse.fromJson)
  }

  def CreateCommit2(body: VersioningCreateCommitRequest, repository_id_repo_id: BigInt)(implicit ec: ExecutionContext): Try[VersioningCreateCommitRequestResponse] = Await.result(CreateCommit2Async(body, repository_id_repo_id), Duration.Inf)

  def CreateRepositoryAsync(body: VersioningRepository, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningSetRepositoryResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningRepository, VersioningSetRepositoryResponse]("POST", basePath + s"/versioning/workspaces/$id_named_id_workspace_name/repositories", __query.toMap, body, VersioningSetRepositoryResponse.fromJson)
  }

  def CreateRepository(body: VersioningRepository, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningSetRepositoryResponse] = Await.result(CreateRepositoryAsync(body, id_named_id_workspace_name), Duration.Inf)

  def DeleteBranchAsync(branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteBranchRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningDeleteBranchRequestResponse]("DELETE", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/branches/$branch", __query.toMap, body, VersioningDeleteBranchRequestResponse.fromJson)
  }

  def DeleteBranch(branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningDeleteBranchRequestResponse] = Await.result(DeleteBranchAsync(branch, repository_id_named_id_name, repository_id_named_id_workspace_name, repository_id_repo_id), Duration.Inf)

  def DeleteBranch2Async(branch: String, repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteBranchRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningDeleteBranchRequestResponse]("DELETE", basePath + s"/versioning/repositories/$repository_id_repo_id/branches/$branch", __query.toMap, body, VersioningDeleteBranchRequestResponse.fromJson)
  }

  def DeleteBranch2(branch: String, repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningDeleteBranchRequestResponse] = Await.result(DeleteBranch2Async(branch, repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def DeleteCommitAsync(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteCommitRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningDeleteCommitRequestResponse]("DELETE", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha", __query.toMap, body, VersioningDeleteCommitRequestResponse.fromJson)
  }

  def DeleteCommit(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningDeleteCommitRequestResponse] = Await.result(DeleteCommitAsync(commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name, repository_id_repo_id), Duration.Inf)

  def DeleteCommit2Async(commit_sha: String, repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteCommitRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningDeleteCommitRequestResponse]("DELETE", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha", __query.toMap, body, VersioningDeleteCommitRequestResponse.fromJson)
  }

  def DeleteCommit2(commit_sha: String, repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningDeleteCommitRequestResponse] = Await.result(DeleteCommit2Async(commit_sha, repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def DeleteRepositoryAsync(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteRepositoryRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningDeleteRepositoryRequestResponse]("DELETE", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name", __query.toMap, body, VersioningDeleteRepositoryRequestResponse.fromJson)
  }

  def DeleteRepository(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningDeleteRepositoryRequestResponse] = Await.result(DeleteRepositoryAsync(repository_id_named_id_name, repository_id_named_id_workspace_name, repository_id_repo_id), Duration.Inf)

  def DeleteRepository2Async(repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteRepositoryRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningDeleteRepositoryRequestResponse]("DELETE", basePath + s"/versioning/repositories/$repository_id_repo_id", __query.toMap, body, VersioningDeleteRepositoryRequestResponse.fromJson)
  }

  def DeleteRepository2(repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningDeleteRepositoryRequestResponse] = Await.result(DeleteRepository2Async(repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def DeleteTagAsync(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, tag: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteTagRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    val body: String = null
    return client.request[String, VersioningDeleteTagRequestResponse]("DELETE", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags/$tag", __query.toMap, body, VersioningDeleteTagRequestResponse.fromJson)
  }

  def DeleteTag(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, tag: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningDeleteTagRequestResponse] = Await.result(DeleteTagAsync(repository_id_named_id_name, repository_id_named_id_workspace_name, tag, repository_id_repo_id), Duration.Inf)

  def DeleteTag2Async(repository_id_repo_id: BigInt, tag: String, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteTagRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    val body: String = null
    return client.request[String, VersioningDeleteTagRequestResponse]("DELETE", basePath + s"/versioning/repositories/$repository_id_repo_id/tags/$tag", __query.toMap, body, VersioningDeleteTagRequestResponse.fromJson)
  }

  def DeleteTag2(repository_id_repo_id: BigInt, tag: String, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningDeleteTagRequestResponse] = Await.result(DeleteTag2Async(repository_id_repo_id, tag, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def GetBranchAsync(branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningGetBranchRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningGetBranchRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/branches/$branch", __query.toMap, body, VersioningGetBranchRequestResponse.fromJson)
  }

  def GetBranch(branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningGetBranchRequestResponse] = Await.result(GetBranchAsync(branch, repository_id_named_id_name, repository_id_named_id_workspace_name, repository_id_repo_id), Duration.Inf)

  def GetBranch2Async(branch: String, repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningGetBranchRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningGetBranchRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/branches/$branch", __query.toMap, body, VersioningGetBranchRequestResponse.fromJson)
  }

  def GetBranch2(branch: String, repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningGetBranchRequestResponse] = Await.result(GetBranch2Async(branch, repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def GetCommitAsync(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningGetCommitRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningGetCommitRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha", __query.toMap, body, VersioningGetCommitRequestResponse.fromJson)
  }

  def GetCommit(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningGetCommitRequestResponse] = Await.result(GetCommitAsync(commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name, repository_id_repo_id), Duration.Inf)

  def GetCommit2Async(commit_sha: String, repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningGetCommitRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningGetCommitRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha", __query.toMap, body, VersioningGetCommitRequestResponse.fromJson)
  }

  def GetCommit2(commit_sha: String, repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningGetCommitRequestResponse] = Await.result(GetCommit2Async(commit_sha, repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def GetCommitComponentAsync(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, location: Option[List[String]]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningGetCommitComponentRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (location.isDefined) __query.update("location", client.toQuery(location.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningGetCommitComponentRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/path", __query.toMap, body, VersioningGetCommitComponentRequestResponse.fromJson)
  }

  def GetCommitComponent(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, location: Option[List[String]]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningGetCommitComponentRequestResponse] = Await.result(GetCommitComponentAsync(commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name, location, repository_id_repo_id), Duration.Inf)

  def GetCommitComponent2Async(commit_sha: String, repository_id_repo_id: BigInt, location: Option[List[String]]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningGetCommitComponentRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (location.isDefined) __query.update("location", client.toQuery(location.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningGetCommitComponentRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/path", __query.toMap, body, VersioningGetCommitComponentRequestResponse.fromJson)
  }

  def GetCommitComponent2(commit_sha: String, repository_id_repo_id: BigInt, location: Option[List[String]]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningGetCommitComponentRequestResponse] = Await.result(GetCommitComponent2Async(commit_sha, repository_id_repo_id, location, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def GetRepositoryAsync(id_named_id_name: String, id_named_id_workspace_name: String, id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningGetRepositoryRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_repo_id.isDefined) __query.update("id.repo_id", client.toQuery(id_repo_id.get))
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningGetRepositoryRequestResponse]("GET", basePath + s"/versioning/workspaces/$id_named_id_workspace_name/repositories/$id_named_id_name", __query.toMap, body, VersioningGetRepositoryRequestResponse.fromJson)
  }

  def GetRepository(id_named_id_name: String, id_named_id_workspace_name: String, id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningGetRepositoryRequestResponse] = Await.result(GetRepositoryAsync(id_named_id_name, id_named_id_workspace_name, id_repo_id), Duration.Inf)

  def GetRepository2Async(id_repo_id: BigInt, id_named_id_name: Option[String]=None, id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningGetRepositoryRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_name.isDefined) __query.update("id.named_id.name", client.toQuery(id_named_id_name.get))
    if (id_named_id_workspace_name.isDefined) __query.update("id.named_id.workspace_name", client.toQuery(id_named_id_workspace_name.get))
    if (id_repo_id == null) throw new Exception("Missing required parameter \"id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningGetRepositoryRequestResponse]("GET", basePath + s"/versioning/repositories/$id_repo_id", __query.toMap, body, VersioningGetRepositoryRequestResponse.fromJson)
  }

  def GetRepository2(id_repo_id: BigInt, id_named_id_name: Option[String]=None, id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningGetRepositoryRequestResponse] = Await.result(GetRepository2Async(id_repo_id, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def GetTagAsync(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, tag: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningGetTagRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    val body: String = null
    return client.request[String, VersioningGetTagRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags/$tag", __query.toMap, body, VersioningGetTagRequestResponse.fromJson)
  }

  def GetTag(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, tag: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningGetTagRequestResponse] = Await.result(GetTagAsync(repository_id_named_id_name, repository_id_named_id_workspace_name, tag, repository_id_repo_id), Duration.Inf)

  def GetTag2Async(repository_id_repo_id: BigInt, tag: String, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningGetTagRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    val body: String = null
    return client.request[String, VersioningGetTagRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/tags/$tag", __query.toMap, body, VersioningGetTagRequestResponse.fromJson)
  }

  def GetTag2(repository_id_repo_id: BigInt, tag: String, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningGetTagRequestResponse] = Await.result(GetTag2Async(repository_id_repo_id, tag, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ListBlobExperimentRunsAsync(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, location: Option[List[String]]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListBlobExperimentRunsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    if (location.isDefined) __query.update("location", client.toQuery(location.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningListBlobExperimentRunsRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/path/runs", __query.toMap, body, VersioningListBlobExperimentRunsRequestResponse.fromJson)
  }

  def ListBlobExperimentRuns(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, location: Option[List[String]]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningListBlobExperimentRunsRequestResponse] = Await.result(ListBlobExperimentRunsAsync(commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name, location, pagination_page_limit, pagination_page_number, repository_id_repo_id), Duration.Inf)

  def ListBlobExperimentRuns2Async(commit_sha: String, repository_id_repo_id: BigInt, location: Option[List[String]]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListBlobExperimentRunsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    if (location.isDefined) __query.update("location", client.toQuery(location.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningListBlobExperimentRunsRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/path/runs", __query.toMap, body, VersioningListBlobExperimentRunsRequestResponse.fromJson)
  }

  def ListBlobExperimentRuns2(commit_sha: String, repository_id_repo_id: BigInt, location: Option[List[String]]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningListBlobExperimentRunsRequestResponse] = Await.result(ListBlobExperimentRuns2Async(commit_sha, repository_id_repo_id, location, pagination_page_limit, pagination_page_number, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ListBranchesAsync(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListBranchesRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningListBranchesRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/branches", __query.toMap, body, VersioningListBranchesRequestResponse.fromJson)
  }

  def ListBranches(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningListBranchesRequestResponse] = Await.result(ListBranchesAsync(repository_id_named_id_name, repository_id_named_id_workspace_name, repository_id_repo_id), Duration.Inf)

  def ListBranches2Async(repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListBranchesRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningListBranchesRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/branches", __query.toMap, body, VersioningListBranchesRequestResponse.fromJson)
  }

  def ListBranches2(repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningListBranchesRequestResponse] = Await.result(ListBranches2Async(repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ListCommitBlobsAsync(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, location_prefix: Option[List[String]]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitBlobsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (location_prefix.isDefined) __query.update("location_prefix", client.toQuery(location_prefix.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningListCommitBlobsRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/blobs", __query.toMap, body, VersioningListCommitBlobsRequestResponse.fromJson)
  }

  def ListCommitBlobs(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, location_prefix: Option[List[String]]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningListCommitBlobsRequestResponse] = Await.result(ListCommitBlobsAsync(commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name, location_prefix, repository_id_repo_id), Duration.Inf)

  def ListCommitBlobs2Async(commit_sha: String, repository_id_repo_id: BigInt, location_prefix: Option[List[String]]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitBlobsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (location_prefix.isDefined) __query.update("location_prefix", client.toQuery(location_prefix.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningListCommitBlobsRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/blobs", __query.toMap, body, VersioningListCommitBlobsRequestResponse.fromJson)
  }

  def ListCommitBlobs2(commit_sha: String, repository_id_repo_id: BigInt, location_prefix: Option[List[String]]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningListCommitBlobsRequestResponse] = Await.result(ListCommitBlobs2Async(commit_sha, repository_id_repo_id, location_prefix, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ListCommitExperimentRunsAsync(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitExperimentRunsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningListCommitExperimentRunsRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/runs", __query.toMap, body, VersioningListCommitExperimentRunsRequestResponse.fromJson)
  }

  def ListCommitExperimentRuns(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningListCommitExperimentRunsRequestResponse] = Await.result(ListCommitExperimentRunsAsync(commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name, pagination_page_limit, pagination_page_number, repository_id_repo_id), Duration.Inf)

  def ListCommitExperimentRuns2Async(commit_sha: String, repository_id_repo_id: BigInt, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitExperimentRunsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningListCommitExperimentRunsRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/runs", __query.toMap, body, VersioningListCommitExperimentRunsRequestResponse.fromJson)
  }

  def ListCommitExperimentRuns2(commit_sha: String, repository_id_repo_id: BigInt, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningListCommitExperimentRunsRequestResponse] = Await.result(ListCommitExperimentRuns2Async(commit_sha, repository_id_repo_id, pagination_page_limit, pagination_page_number, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ListCommitsAsync(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, commit_base: Option[String]=None, commit_head: Option[String]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    if (commit_base.isDefined) __query.update("commit_base", client.toQuery(commit_base.get))
    if (commit_head.isDefined) __query.update("commit_head", client.toQuery(commit_head.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningListCommitsRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits", __query.toMap, body, VersioningListCommitsRequestResponse.fromJson)
  }

  def ListCommits(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, commit_base: Option[String]=None, commit_head: Option[String]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningListCommitsRequestResponse] = Await.result(ListCommitsAsync(repository_id_named_id_name, repository_id_named_id_workspace_name, commit_base, commit_head, pagination_page_limit, pagination_page_number, repository_id_repo_id), Duration.Inf)

  def ListCommits2Async(repository_id_repo_id: BigInt, commit_base: Option[String]=None, commit_head: Option[String]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    if (commit_base.isDefined) __query.update("commit_base", client.toQuery(commit_base.get))
    if (commit_head.isDefined) __query.update("commit_head", client.toQuery(commit_head.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningListCommitsRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits", __query.toMap, body, VersioningListCommitsRequestResponse.fromJson)
  }

  def ListCommits2(repository_id_repo_id: BigInt, commit_base: Option[String]=None, commit_head: Option[String]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningListCommitsRequestResponse] = Await.result(ListCommits2Async(repository_id_repo_id, commit_base, commit_head, pagination_page_limit, pagination_page_number, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ListCommitsLogAsync(branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, commit_sha: Option[String]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitsLogRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (commit_sha.isDefined) __query.update("commit_sha", client.toQuery(commit_sha.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningListCommitsLogRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/branches/$branch/log", __query.toMap, body, VersioningListCommitsLogRequestResponse.fromJson)
  }

  def ListCommitsLog(branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, commit_sha: Option[String]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningListCommitsLogRequestResponse] = Await.result(ListCommitsLogAsync(branch, repository_id_named_id_name, repository_id_named_id_workspace_name, commit_sha, repository_id_repo_id), Duration.Inf)

  def ListCommitsLog2Async(branch: String, repository_id_repo_id: BigInt, commit_sha: Option[String]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitsLogRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (commit_sha.isDefined) __query.update("commit_sha", client.toQuery(commit_sha.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningListCommitsLogRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/branches/$branch/log", __query.toMap, body, VersioningListCommitsLogRequestResponse.fromJson)
  }

  def ListCommitsLog2(branch: String, repository_id_repo_id: BigInt, commit_sha: Option[String]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningListCommitsLogRequestResponse] = Await.result(ListCommitsLog2Async(branch, repository_id_repo_id, commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ListCommitsLog3Async(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, branch: Option[String]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitsLogRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (branch.isDefined) __query.update("branch", client.toQuery(branch.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningListCommitsLogRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/log", __query.toMap, body, VersioningListCommitsLogRequestResponse.fromJson)
  }

  def ListCommitsLog3(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, branch: Option[String]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningListCommitsLogRequestResponse] = Await.result(ListCommitsLog3Async(commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name, branch, repository_id_repo_id), Duration.Inf)

  def ListCommitsLog4Async(commit_sha: String, repository_id_repo_id: BigInt, branch: Option[String]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitsLogRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (branch.isDefined) __query.update("branch", client.toQuery(branch.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningListCommitsLogRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/log", __query.toMap, body, VersioningListCommitsLogRequestResponse.fromJson)
  }

  def ListCommitsLog4(commit_sha: String, repository_id_repo_id: BigInt, branch: Option[String]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningListCommitsLogRequestResponse] = Await.result(ListCommitsLog4Async(commit_sha, repository_id_repo_id, branch, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ListRepositoriesAsync(workspace_name: String, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListRepositoriesRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    if (workspace_name == null) throw new Exception("Missing required parameter \"workspace_name\"")
    val body: String = null
    return client.request[String, VersioningListRepositoriesRequestResponse]("GET", basePath + s"/versioning/workspaces/$workspace_name/repositories", __query.toMap, body, VersioningListRepositoriesRequestResponse.fromJson)
  }

  def ListRepositories(workspace_name: String, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningListRepositoriesRequestResponse] = Await.result(ListRepositoriesAsync(workspace_name, pagination_page_limit, pagination_page_number), Duration.Inf)

  def ListRepositories2Async(pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListRepositoriesRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (workspace_name.isDefined) __query.update("workspace_name", client.toQuery(workspace_name.get))
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    val body: String = null
    return client.request[String, VersioningListRepositoriesRequestResponse]("GET", basePath + s"/versioning/repositories", __query.toMap, body, VersioningListRepositoriesRequestResponse.fromJson)
  }

  def ListRepositories2(pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningListRepositoriesRequestResponse] = Await.result(ListRepositories2Async(pagination_page_limit, pagination_page_number, workspace_name), Duration.Inf)

  def ListTagsAsync(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListTagsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningListTagsRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags", __query.toMap, body, VersioningListTagsRequestResponse.fromJson)
  }

  def ListTags(repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningListTagsRequestResponse] = Await.result(ListTagsAsync(repository_id_named_id_name, repository_id_named_id_workspace_name, repository_id_repo_id), Duration.Inf)

  def ListTags2Async(repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[VersioningListTagsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningListTagsRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/tags", __query.toMap, body, VersioningListTagsRequestResponse.fromJson)
  }

  def ListTags2(repository_id_repo_id: BigInt, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[VersioningListTagsRequestResponse] = Await.result(ListTags2Async(repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def MergeRepositoryCommitsAsync(body: VersioningMergeRepositoryCommitsRequest, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningMergeRepositoryCommitsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningMergeRepositoryCommitsRequest, VersioningMergeRepositoryCommitsRequestResponse]("POST", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/merge", __query.toMap, body, VersioningMergeRepositoryCommitsRequestResponse.fromJson)
  }

  def MergeRepositoryCommits(body: VersioningMergeRepositoryCommitsRequest, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningMergeRepositoryCommitsRequestResponse] = Await.result(MergeRepositoryCommitsAsync(body, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def MergeRepositoryCommits2Async(body: VersioningMergeRepositoryCommitsRequest, repository_id_repo_id: BigInt)(implicit ec: ExecutionContext): Future[Try[VersioningMergeRepositoryCommitsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningMergeRepositoryCommitsRequest, VersioningMergeRepositoryCommitsRequestResponse]("POST", basePath + s"/versioning/repositories/$repository_id_repo_id/merge", __query.toMap, body, VersioningMergeRepositoryCommitsRequestResponse.fromJson)
  }

  def MergeRepositoryCommits2(body: VersioningMergeRepositoryCommitsRequest, repository_id_repo_id: BigInt)(implicit ec: ExecutionContext): Try[VersioningMergeRepositoryCommitsRequestResponse] = Await.result(MergeRepositoryCommits2Async(body, repository_id_repo_id), Duration.Inf)

  def RevertRepositoryCommitsAsync(body: VersioningRevertRepositoryCommitsRequest, commit_to_revert_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningRevertRepositoryCommitsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_to_revert_sha == null) throw new Exception("Missing required parameter \"commit_to_revert_sha\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningRevertRepositoryCommitsRequest, VersioningRevertRepositoryCommitsRequestResponse]("POST", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_to_revert_sha/revert", __query.toMap, body, VersioningRevertRepositoryCommitsRequestResponse.fromJson)
  }

  def RevertRepositoryCommits(body: VersioningRevertRepositoryCommitsRequest, commit_to_revert_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningRevertRepositoryCommitsRequestResponse] = Await.result(RevertRepositoryCommitsAsync(body, commit_to_revert_sha, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def RevertRepositoryCommits2Async(body: VersioningRevertRepositoryCommitsRequest, commit_to_revert_sha: String, repository_id_repo_id: BigInt)(implicit ec: ExecutionContext): Future[Try[VersioningRevertRepositoryCommitsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_to_revert_sha == null) throw new Exception("Missing required parameter \"commit_to_revert_sha\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningRevertRepositoryCommitsRequest, VersioningRevertRepositoryCommitsRequestResponse]("POST", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_to_revert_sha/revert", __query.toMap, body, VersioningRevertRepositoryCommitsRequestResponse.fromJson)
  }

  def RevertRepositoryCommits2(body: VersioningRevertRepositoryCommitsRequest, commit_to_revert_sha: String, repository_id_repo_id: BigInt)(implicit ec: ExecutionContext): Try[VersioningRevertRepositoryCommitsRequestResponse] = Await.result(RevertRepositoryCommits2Async(body, commit_to_revert_sha, repository_id_repo_id), Duration.Inf)

  def SetBranchAsync(body: String, branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningSetBranchRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[String, VersioningSetBranchRequestResponse]("PUT", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/branches/$branch", __query.toMap, body, VersioningSetBranchRequestResponse.fromJson)
  }

  def SetBranch(body: String, branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningSetBranchRequestResponse] = Await.result(SetBranchAsync(body, branch, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def SetBranch2Async(body: String, branch: String, repository_id_repo_id: BigInt)(implicit ec: ExecutionContext): Future[Try[VersioningSetBranchRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[String, VersioningSetBranchRequestResponse]("PUT", basePath + s"/versioning/repositories/$repository_id_repo_id/branches/$branch", __query.toMap, body, VersioningSetBranchRequestResponse.fromJson)
  }

  def SetBranch2(body: String, branch: String, repository_id_repo_id: BigInt)(implicit ec: ExecutionContext): Try[VersioningSetBranchRequestResponse] = Await.result(SetBranch2Async(body, branch, repository_id_repo_id), Duration.Inf)

  def SetTagAsync(body: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, tag: String)(implicit ec: ExecutionContext): Future[Try[VersioningSetTagRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[String, VersioningSetTagRequestResponse]("PUT", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags/$tag", __query.toMap, body, VersioningSetTagRequestResponse.fromJson)
  }

  def SetTag(body: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, tag: String)(implicit ec: ExecutionContext): Try[VersioningSetTagRequestResponse] = Await.result(SetTagAsync(body, repository_id_named_id_name, repository_id_named_id_workspace_name, tag), Duration.Inf)

  def SetTag2Async(body: String, repository_id_repo_id: BigInt, tag: String)(implicit ec: ExecutionContext): Future[Try[VersioningSetTagRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[String, VersioningSetTagRequestResponse]("PUT", basePath + s"/versioning/repositories/$repository_id_repo_id/tags/$tag", __query.toMap, body, VersioningSetTagRequestResponse.fromJson)
  }

  def SetTag2(body: String, repository_id_repo_id: BigInt, tag: String)(implicit ec: ExecutionContext): Try[VersioningSetTagRequestResponse] = Await.result(SetTag2Async(body, repository_id_repo_id, tag), Duration.Inf)

  def UpdateRepositoryAsync(body: VersioningRepository, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningSetRepositoryResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningRepository, VersioningSetRepositoryResponse]("PUT", basePath + s"/versioning/workspaces/$id_named_id_workspace_name/repositories/$id_named_id_name", __query.toMap, body, VersioningSetRepositoryResponse.fromJson)
  }

  def UpdateRepository(body: VersioningRepository, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningSetRepositoryResponse] = Await.result(UpdateRepositoryAsync(body, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def UpdateRepository2Async(body: VersioningRepository, id_repo_id: BigInt)(implicit ec: ExecutionContext): Future[Try[VersioningSetRepositoryResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_repo_id == null) throw new Exception("Missing required parameter \"id_repo_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningRepository, VersioningSetRepositoryResponse]("PUT", basePath + s"/versioning/repositories/$id_repo_id", __query.toMap, body, VersioningSetRepositoryResponse.fromJson)
  }

  def UpdateRepository2(body: VersioningRepository, id_repo_id: BigInt)(implicit ec: ExecutionContext): Try[VersioningSetRepositoryResponse] = Await.result(UpdateRepository2Async(body, id_repo_id), Duration.Inf)

  def commitMultipartVersionedBlobArtifactAsync(body: VersioningCommitMultipartVersionedBlobArtifact)(implicit ec: ExecutionContext): Future[Try[VersioningCommitMultipartVersionedBlobArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningCommitMultipartVersionedBlobArtifact, VersioningCommitMultipartVersionedBlobArtifactResponse]("POST", basePath + s"/versioning/commitMultipartVersionedBlobArtifact", __query.toMap, body, VersioningCommitMultipartVersionedBlobArtifactResponse.fromJson)
  }

  def commitMultipartVersionedBlobArtifact(body: VersioningCommitMultipartVersionedBlobArtifact)(implicit ec: ExecutionContext): Try[VersioningCommitMultipartVersionedBlobArtifactResponse] = Await.result(commitMultipartVersionedBlobArtifactAsync(body), Duration.Inf)

  def commitVersionedBlobArtifactPartAsync(body: VersioningCommitVersionedBlobArtifactPart)(implicit ec: ExecutionContext): Future[Try[VersioningCommitVersionedBlobArtifactPartResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningCommitVersionedBlobArtifactPart, VersioningCommitVersionedBlobArtifactPartResponse]("POST", basePath + s"/versioning/commitVersionedBlobArtifactPart", __query.toMap, body, VersioningCommitVersionedBlobArtifactPartResponse.fromJson)
  }

  def commitVersionedBlobArtifactPart(body: VersioningCommitVersionedBlobArtifactPart)(implicit ec: ExecutionContext): Try[VersioningCommitVersionedBlobArtifactPartResponse] = Await.result(commitVersionedBlobArtifactPartAsync(body), Duration.Inf)

  def findRepositoriesAsync(body: VersioningFindRepositories, workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningFindRepositoriesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (workspace_name == null) throw new Exception("Missing required parameter \"workspace_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningFindRepositories, VersioningFindRepositoriesResponse]("POST", basePath + s"/versioning/workspaces/$workspace_name/findRepositories", __query.toMap, body, VersioningFindRepositoriesResponse.fromJson)
  }

  def findRepositories(body: VersioningFindRepositories, workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningFindRepositoriesResponse] = Await.result(findRepositoriesAsync(body, workspace_name), Duration.Inf)

  def findRepositoriesBlobsAsync(body: VersioningFindRepositoriesBlobs, workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningFindRepositoriesBlobsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (workspace_name == null) throw new Exception("Missing required parameter \"workspace_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningFindRepositoriesBlobs, VersioningFindRepositoriesBlobsResponse]("POST", basePath + s"/versioning/workspaces/$workspace_name/findRepositoriesBlobs", __query.toMap, body, VersioningFindRepositoriesBlobsResponse.fromJson)
  }

  def findRepositoriesBlobs(body: VersioningFindRepositoriesBlobs, workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningFindRepositoriesBlobsResponse] = Await.result(findRepositoriesBlobsAsync(body, workspace_name), Duration.Inf)

  def getCommittedVersionedBlobArtifactPartsAsync(commit_sha: Option[String]=None, location: Option[List[String]]=None, path_dataset_component_blob_path: Option[String]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[VersioningGetCommittedVersionedBlobArtifactPartsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (commit_sha.isDefined) __query.update("commit_sha", client.toQuery(commit_sha.get))
    if (location.isDefined) __query.update("location", client.toQuery(location.get))
    if (path_dataset_component_blob_path.isDefined) __query.update("path_dataset_component_blob_path", client.toQuery(path_dataset_component_blob_path.get))
    val body: String = null
    return client.request[String, VersioningGetCommittedVersionedBlobArtifactPartsResponse]("GET", basePath + s"/versioning/getCommittedVersionedBlobArtifactParts", __query.toMap, body, VersioningGetCommittedVersionedBlobArtifactPartsResponse.fromJson)
  }

  def getCommittedVersionedBlobArtifactParts(commit_sha: Option[String]=None, location: Option[List[String]]=None, path_dataset_component_blob_path: Option[String]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[VersioningGetCommittedVersionedBlobArtifactPartsResponse] = Await.result(getCommittedVersionedBlobArtifactPartsAsync(commit_sha, location, path_dataset_component_blob_path, repository_id_named_id_name, repository_id_named_id_workspace_name, repository_id_repo_id), Duration.Inf)

  def getUrlForBlobVersionedAsync(body: VersioningGetUrlForBlobVersioned, commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningGetUrlForBlobVersionedResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningGetUrlForBlobVersioned, VersioningGetUrlForBlobVersionedResponse]("POST", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/getUrlForBlobVersioned", __query.toMap, body, VersioningGetUrlForBlobVersionedResponse.fromJson)
  }

  def getUrlForBlobVersioned(body: VersioningGetUrlForBlobVersioned, commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningGetUrlForBlobVersionedResponse] = Await.result(getUrlForBlobVersionedAsync(body, commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def getUrlForBlobVersioned2Async(body: VersioningGetUrlForBlobVersioned, commit_sha: String, repository_id_repo_id: BigInt)(implicit ec: ExecutionContext): Future[Try[VersioningGetUrlForBlobVersionedResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningGetUrlForBlobVersioned, VersioningGetUrlForBlobVersionedResponse]("POST", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/getUrlForBlobVersioned", __query.toMap, body, VersioningGetUrlForBlobVersionedResponse.fromJson)
  }

  def getUrlForBlobVersioned2(body: VersioningGetUrlForBlobVersioned, commit_sha: String, repository_id_repo_id: BigInt)(implicit ec: ExecutionContext): Try[VersioningGetUrlForBlobVersionedResponse] = Await.result(getUrlForBlobVersioned2Async(body, commit_sha, repository_id_repo_id), Duration.Inf)

}
