// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.versioning.model._

class VersioningServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def ComputeRepositoryDiffAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, repository_id_repo_id: String, commit_a: String, commit_b: String, location_prefix: List[String])(implicit ec: ExecutionContext): Future[Try[VersioningComputeRepositoryDiffRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id),
      "commit_a" -> client.toQuery(commit_a),
      "commit_b" -> client.toQuery(commit_b),
      "location_prefix" -> client.toQuery(location_prefix)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningComputeRepositoryDiffRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/diff", __query, body, VersioningComputeRepositoryDiffRequestResponse.fromJson)
  }

  def ComputeRepositoryDiff(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, repository_id_repo_id: String, commit_a: String, commit_b: String, location_prefix: List[String])(implicit ec: ExecutionContext): Try[VersioningComputeRepositoryDiffRequestResponse] = Await.result(ComputeRepositoryDiffAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, repository_id_repo_id, commit_a, commit_b, location_prefix), Duration.Inf)

  def ComputeRepositoryDiff2Async(repository_id_repo_id: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, commit_a: String, commit_b: String, location_prefix: List[String])(implicit ec: ExecutionContext): Future[Try[VersioningComputeRepositoryDiffRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name),
      "commit_a" -> client.toQuery(commit_a),
      "commit_b" -> client.toQuery(commit_b),
      "location_prefix" -> client.toQuery(location_prefix)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningComputeRepositoryDiffRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/diff", __query, body, VersioningComputeRepositoryDiffRequestResponse.fromJson)
  }

  def ComputeRepositoryDiff2(repository_id_repo_id: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, commit_a: String, commit_b: String, location_prefix: List[String])(implicit ec: ExecutionContext): Try[VersioningComputeRepositoryDiffRequestResponse] = Await.result(ComputeRepositoryDiff2Async(repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name, commit_a, commit_b, location_prefix), Duration.Inf)

  def CreateCommitAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, body: VersioningCreateCommitRequest)(implicit ec: ExecutionContext): Future[Try[VersioningCreateCommitRequestResponse]] = {
    val __query = Map[String,String](
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningCreateCommitRequest, VersioningCreateCommitRequestResponse]("POST", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits", __query, body, VersioningCreateCommitRequestResponse.fromJson)
  }

  def CreateCommit(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, body: VersioningCreateCommitRequest)(implicit ec: ExecutionContext): Try[VersioningCreateCommitRequestResponse] = Await.result(CreateCommitAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, body), Duration.Inf)

  def CreateCommit2Async(repository_id_repo_id: String, body: VersioningCreateCommitRequest)(implicit ec: ExecutionContext): Future[Try[VersioningCreateCommitRequestResponse]] = {
    val __query = Map[String,String](
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningCreateCommitRequest, VersioningCreateCommitRequestResponse]("POST", basePath + s"/versioning/repositories/$repository_id_repo_id/commits", __query, body, VersioningCreateCommitRequestResponse.fromJson)
  }

  def CreateCommit2(repository_id_repo_id: String, body: VersioningCreateCommitRequest)(implicit ec: ExecutionContext): Try[VersioningCreateCommitRequestResponse] = Await.result(CreateCommit2Async(repository_id_repo_id, body), Duration.Inf)

  def CreateRepositoryAsync(id_named_id_workspace_name: String, body: VersioningRepository)(implicit ec: ExecutionContext): Future[Try[VersioningSetRepositoryResponse]] = {
    val __query = Map[String,String](
    )
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningRepository, VersioningSetRepositoryResponse]("POST", basePath + s"/versioning/workspaces/$id_named_id_workspace_name/repositories", __query, body, VersioningSetRepositoryResponse.fromJson)
  }

  def CreateRepository(id_named_id_workspace_name: String, body: VersioningRepository)(implicit ec: ExecutionContext): Try[VersioningSetRepositoryResponse] = Await.result(CreateRepositoryAsync(id_named_id_workspace_name, body), Duration.Inf)

  def DeleteBranchAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, branch: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteBranchRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningDeleteBranchRequestResponse]("DELETE", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/branches/$branch", __query, body, VersioningDeleteBranchRequestResponse.fromJson)
  }

  def DeleteBranch(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, branch: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Try[VersioningDeleteBranchRequestResponse] = Await.result(DeleteBranchAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, branch, repository_id_repo_id), Duration.Inf)

  def DeleteBranch2Async(repository_id_repo_id: String, branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteBranchRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningDeleteBranchRequestResponse]("DELETE", basePath + s"/versioning/repositories/$repository_id_repo_id/branches/$branch", __query, body, VersioningDeleteBranchRequestResponse.fromJson)
  }

  def DeleteBranch2(repository_id_repo_id: String, branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningDeleteBranchRequestResponse] = Await.result(DeleteBranch2Async(repository_id_repo_id, branch, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def DeleteCommitAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, commit_sha: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteCommitRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningDeleteCommitRequestResponse]("DELETE", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha", __query, body, VersioningDeleteCommitRequestResponse.fromJson)
  }

  def DeleteCommit(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, commit_sha: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Try[VersioningDeleteCommitRequestResponse] = Await.result(DeleteCommitAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, commit_sha, repository_id_repo_id), Duration.Inf)

  def DeleteCommit2Async(repository_id_repo_id: String, commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteCommitRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningDeleteCommitRequestResponse]("DELETE", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha", __query, body, VersioningDeleteCommitRequestResponse.fromJson)
  }

  def DeleteCommit2(repository_id_repo_id: String, commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningDeleteCommitRequestResponse] = Await.result(DeleteCommit2Async(repository_id_repo_id, commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def DeleteRepositoryAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteRepositoryRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningDeleteRepositoryRequestResponse]("DELETE", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name", __query, body, VersioningDeleteRepositoryRequestResponse.fromJson)
  }

  def DeleteRepository(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Try[VersioningDeleteRepositoryRequestResponse] = Await.result(DeleteRepositoryAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, repository_id_repo_id), Duration.Inf)

  def DeleteRepository2Async(repository_id_repo_id: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteRepositoryRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningDeleteRepositoryRequestResponse]("DELETE", basePath + s"/versioning/repositories/$repository_id_repo_id", __query, body, VersioningDeleteRepositoryRequestResponse.fromJson)
  }

  def DeleteRepository2(repository_id_repo_id: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningDeleteRepositoryRequestResponse] = Await.result(DeleteRepository2Async(repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def DeleteTagAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, tag: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteTagRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    val body: String = null
    return client.request[String, VersioningDeleteTagRequestResponse]("DELETE", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags/$tag", __query, body, VersioningDeleteTagRequestResponse.fromJson)
  }

  def DeleteTag(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, tag: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Try[VersioningDeleteTagRequestResponse] = Await.result(DeleteTagAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, tag, repository_id_repo_id), Duration.Inf)

  def DeleteTag2Async(repository_id_repo_id: String, tag: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningDeleteTagRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    val body: String = null
    return client.request[String, VersioningDeleteTagRequestResponse]("DELETE", basePath + s"/versioning/repositories/$repository_id_repo_id/tags/$tag", __query, body, VersioningDeleteTagRequestResponse.fromJson)
  }

  def DeleteTag2(repository_id_repo_id: String, tag: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningDeleteTagRequestResponse] = Await.result(DeleteTag2Async(repository_id_repo_id, tag, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def GetBranchAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, branch: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Future[Try[VersioningGetBranchRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningGetBranchRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/branches/$branch", __query, body, VersioningGetBranchRequestResponse.fromJson)
  }

  def GetBranch(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, branch: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Try[VersioningGetBranchRequestResponse] = Await.result(GetBranchAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, branch, repository_id_repo_id), Duration.Inf)

  def GetBranch2Async(repository_id_repo_id: String, branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningGetBranchRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningGetBranchRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/branches/$branch", __query, body, VersioningGetBranchRequestResponse.fromJson)
  }

  def GetBranch2(repository_id_repo_id: String, branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningGetBranchRequestResponse] = Await.result(GetBranch2Async(repository_id_repo_id, branch, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def GetCommitAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, commit_sha: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Future[Try[VersioningGetCommitRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningGetCommitRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha", __query, body, VersioningGetCommitRequestResponse.fromJson)
  }

  def GetCommit(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, commit_sha: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Try[VersioningGetCommitRequestResponse] = Await.result(GetCommitAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, commit_sha, repository_id_repo_id), Duration.Inf)

  def GetCommit2Async(repository_id_repo_id: String, commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningGetCommitRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningGetCommitRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha", __query, body, VersioningGetCommitRequestResponse.fromJson)
  }

  def GetCommit2(repository_id_repo_id: String, commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningGetCommitRequestResponse] = Await.result(GetCommit2Async(repository_id_repo_id, commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def GetCommitComponentAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, commit_sha: String, repository_id_repo_id: String, location: List[String])(implicit ec: ExecutionContext): Future[Try[VersioningGetCommitComponentRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id),
      "location" -> client.toQuery(location)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningGetCommitComponentRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/path", __query, body, VersioningGetCommitComponentRequestResponse.fromJson)
  }

  def GetCommitComponent(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, commit_sha: String, repository_id_repo_id: String, location: List[String])(implicit ec: ExecutionContext): Try[VersioningGetCommitComponentRequestResponse] = Await.result(GetCommitComponentAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, commit_sha, repository_id_repo_id, location), Duration.Inf)

  def GetCommitComponent2Async(repository_id_repo_id: String, commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, location: List[String])(implicit ec: ExecutionContext): Future[Try[VersioningGetCommitComponentRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name),
      "location" -> client.toQuery(location)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningGetCommitComponentRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/path", __query, body, VersioningGetCommitComponentRequestResponse.fromJson)
  }

  def GetCommitComponent2(repository_id_repo_id: String, commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, location: List[String])(implicit ec: ExecutionContext): Try[VersioningGetCommitComponentRequestResponse] = Await.result(GetCommitComponent2Async(repository_id_repo_id, commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name, location), Duration.Inf)

  def GetRepositoryAsync(id_named_id_workspace_name: String, id_named_id_name: String, id_repo_id: String)(implicit ec: ExecutionContext): Future[Try[VersioningGetRepositoryRequestResponse]] = {
    val __query = Map[String,String](
      "id.repo_id" -> client.toQuery(id_repo_id)
    )
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningGetRepositoryRequestResponse]("GET", basePath + s"/versioning/workspaces/$id_named_id_workspace_name/repositories/$id_named_id_name", __query, body, VersioningGetRepositoryRequestResponse.fromJson)
  }

  def GetRepository(id_named_id_workspace_name: String, id_named_id_name: String, id_repo_id: String)(implicit ec: ExecutionContext): Try[VersioningGetRepositoryRequestResponse] = Await.result(GetRepositoryAsync(id_named_id_workspace_name, id_named_id_name, id_repo_id), Duration.Inf)

  def GetRepository2Async(id_repo_id: String, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningGetRepositoryRequestResponse]] = {
    val __query = Map[String,String](
      "id.named_id.name" -> client.toQuery(id_named_id_name),
      "id.named_id.workspace_name" -> client.toQuery(id_named_id_workspace_name)
    )
    if (id_repo_id == null) throw new Exception("Missing required parameter \"id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningGetRepositoryRequestResponse]("GET", basePath + s"/versioning/repositories/$id_repo_id", __query, body, VersioningGetRepositoryRequestResponse.fromJson)
  }

  def GetRepository2(id_repo_id: String, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningGetRepositoryRequestResponse] = Await.result(GetRepository2Async(id_repo_id, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def GetTagAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, tag: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Future[Try[VersioningGetTagRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    val body: String = null
    return client.request[String, VersioningGetTagRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags/$tag", __query, body, VersioningGetTagRequestResponse.fromJson)
  }

  def GetTag(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, tag: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Try[VersioningGetTagRequestResponse] = Await.result(GetTagAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, tag, repository_id_repo_id), Duration.Inf)

  def GetTag2Async(repository_id_repo_id: String, tag: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningGetTagRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    val body: String = null
    return client.request[String, VersioningGetTagRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/tags/$tag", __query, body, VersioningGetTagRequestResponse.fromJson)
  }

  def GetTag2(repository_id_repo_id: String, tag: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningGetTagRequestResponse] = Await.result(GetTag2Async(repository_id_repo_id, tag, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ListBranchCommitsAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, branch: String, repository_id_repo_id: String, pagination_page_number: BigInt, pagination_page_limit: BigInt)(implicit ec: ExecutionContext): Future[Try[VersioningListBranchCommitsRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id),
      "pagination.page_number" -> client.toQuery(pagination_page_number),
      "pagination.page_limit" -> client.toQuery(pagination_page_limit)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningListBranchCommitsRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/branches/$branch/commits", __query, body, VersioningListBranchCommitsRequestResponse.fromJson)
  }

  def ListBranchCommits(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, branch: String, repository_id_repo_id: String, pagination_page_number: BigInt, pagination_page_limit: BigInt)(implicit ec: ExecutionContext): Try[VersioningListBranchCommitsRequestResponse] = Await.result(ListBranchCommitsAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, branch, repository_id_repo_id, pagination_page_number, pagination_page_limit), Duration.Inf)

  def ListBranchCommits2Async(repository_id_repo_id: String, branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, pagination_page_number: BigInt, pagination_page_limit: BigInt)(implicit ec: ExecutionContext): Future[Try[VersioningListBranchCommitsRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name),
      "pagination.page_number" -> client.toQuery(pagination_page_number),
      "pagination.page_limit" -> client.toQuery(pagination_page_limit)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    val body: String = null
    return client.request[String, VersioningListBranchCommitsRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/branches/$branch/commits", __query, body, VersioningListBranchCommitsRequestResponse.fromJson)
  }

  def ListBranchCommits2(repository_id_repo_id: String, branch: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, pagination_page_number: BigInt, pagination_page_limit: BigInt)(implicit ec: ExecutionContext): Try[VersioningListBranchCommitsRequestResponse] = Await.result(ListBranchCommits2Async(repository_id_repo_id, branch, repository_id_named_id_name, repository_id_named_id_workspace_name, pagination_page_number, pagination_page_limit), Duration.Inf)

  def ListBranchesAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Future[Try[VersioningListBranchesRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningListBranchesRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/branches", __query, body, VersioningListBranchesRequestResponse.fromJson)
  }

  def ListBranches(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Try[VersioningListBranchesRequestResponse] = Await.result(ListBranchesAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, repository_id_repo_id), Duration.Inf)

  def ListBranches2Async(repository_id_repo_id: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningListBranchesRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningListBranchesRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/branches", __query, body, VersioningListBranchesRequestResponse.fromJson)
  }

  def ListBranches2(repository_id_repo_id: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningListBranchesRequestResponse] = Await.result(ListBranches2Async(repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ListCommitBlobsAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, commit_sha: String, repository_id_repo_id: String, pagination_page_number: BigInt, pagination_page_limit: BigInt, location_prefix: List[String])(implicit ec: ExecutionContext): Future[Try[VersioningListCommitBlobsRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id),
      "pagination.page_number" -> client.toQuery(pagination_page_number),
      "pagination.page_limit" -> client.toQuery(pagination_page_limit),
      "location_prefix" -> client.toQuery(location_prefix)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningListCommitBlobsRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/blobs", __query, body, VersioningListCommitBlobsRequestResponse.fromJson)
  }

  def ListCommitBlobs(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, commit_sha: String, repository_id_repo_id: String, pagination_page_number: BigInt, pagination_page_limit: BigInt, location_prefix: List[String])(implicit ec: ExecutionContext): Try[VersioningListCommitBlobsRequestResponse] = Await.result(ListCommitBlobsAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, commit_sha, repository_id_repo_id, pagination_page_number, pagination_page_limit, location_prefix), Duration.Inf)

  def ListCommitBlobs2Async(repository_id_repo_id: String, commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, pagination_page_number: BigInt, pagination_page_limit: BigInt, location_prefix: List[String])(implicit ec: ExecutionContext): Future[Try[VersioningListCommitBlobsRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name),
      "pagination.page_number" -> client.toQuery(pagination_page_number),
      "pagination.page_limit" -> client.toQuery(pagination_page_limit),
      "location_prefix" -> client.toQuery(location_prefix)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, VersioningListCommitBlobsRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/blobs", __query, body, VersioningListCommitBlobsRequestResponse.fromJson)
  }

  def ListCommitBlobs2(repository_id_repo_id: String, commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, pagination_page_number: BigInt, pagination_page_limit: BigInt, location_prefix: List[String])(implicit ec: ExecutionContext): Try[VersioningListCommitBlobsRequestResponse] = Await.result(ListCommitBlobs2Async(repository_id_repo_id, commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name, pagination_page_number, pagination_page_limit, location_prefix), Duration.Inf)

  def ListCommitsAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, repository_id_repo_id: String, pagination_page_number: BigInt, pagination_page_limit: BigInt, commit_base: String, commit_head: String)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitsRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id),
      "pagination.page_number" -> client.toQuery(pagination_page_number),
      "pagination.page_limit" -> client.toQuery(pagination_page_limit),
      "commit_base" -> client.toQuery(commit_base),
      "commit_head" -> client.toQuery(commit_head)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningListCommitsRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits", __query, body, VersioningListCommitsRequestResponse.fromJson)
  }

  def ListCommits(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, repository_id_repo_id: String, pagination_page_number: BigInt, pagination_page_limit: BigInt, commit_base: String, commit_head: String)(implicit ec: ExecutionContext): Try[VersioningListCommitsRequestResponse] = Await.result(ListCommitsAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, repository_id_repo_id, pagination_page_number, pagination_page_limit, commit_base, commit_head), Duration.Inf)

  def ListCommits2Async(repository_id_repo_id: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, pagination_page_number: BigInt, pagination_page_limit: BigInt, commit_base: String, commit_head: String)(implicit ec: ExecutionContext): Future[Try[VersioningListCommitsRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name),
      "pagination.page_number" -> client.toQuery(pagination_page_number),
      "pagination.page_limit" -> client.toQuery(pagination_page_limit),
      "commit_base" -> client.toQuery(commit_base),
      "commit_head" -> client.toQuery(commit_head)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningListCommitsRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits", __query, body, VersioningListCommitsRequestResponse.fromJson)
  }

  def ListCommits2(repository_id_repo_id: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, pagination_page_number: BigInt, pagination_page_limit: BigInt, commit_base: String, commit_head: String)(implicit ec: ExecutionContext): Try[VersioningListCommitsRequestResponse] = Await.result(ListCommits2Async(repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name, pagination_page_number, pagination_page_limit, commit_base, commit_head), Duration.Inf)

  def ListRepositoriesAsync(workspace_name: String, pagination_page_number: BigInt, pagination_page_limit: BigInt)(implicit ec: ExecutionContext): Future[Try[VersioningListRepositoriesRequestResponse]] = {
    val __query = Map[String,String](
      "pagination.page_number" -> client.toQuery(pagination_page_number),
      "pagination.page_limit" -> client.toQuery(pagination_page_limit)
    )
    if (workspace_name == null) throw new Exception("Missing required parameter \"workspace_name\"")
    val body: String = null
    return client.request[String, VersioningListRepositoriesRequestResponse]("GET", basePath + s"/versioning/workspaces/$workspace_name/repositories", __query, body, VersioningListRepositoriesRequestResponse.fromJson)
  }

  def ListRepositories(workspace_name: String, pagination_page_number: BigInt, pagination_page_limit: BigInt)(implicit ec: ExecutionContext): Try[VersioningListRepositoriesRequestResponse] = Await.result(ListRepositoriesAsync(workspace_name, pagination_page_number, pagination_page_limit), Duration.Inf)

  def ListRepositories2Async(workspace_name: String, pagination_page_number: BigInt, pagination_page_limit: BigInt)(implicit ec: ExecutionContext): Future[Try[VersioningListRepositoriesRequestResponse]] = {
    val __query = Map[String,String](
      "workspace_name" -> client.toQuery(workspace_name),
      "pagination.page_number" -> client.toQuery(pagination_page_number),
      "pagination.page_limit" -> client.toQuery(pagination_page_limit)
    )
    val body: String = null
    return client.request[String, VersioningListRepositoriesRequestResponse]("GET", basePath + s"/versioning/repositories", __query, body, VersioningListRepositoriesRequestResponse.fromJson)
  }

  def ListRepositories2(workspace_name: String, pagination_page_number: BigInt, pagination_page_limit: BigInt)(implicit ec: ExecutionContext): Try[VersioningListRepositoriesRequestResponse] = Await.result(ListRepositories2Async(workspace_name, pagination_page_number, pagination_page_limit), Duration.Inf)

  def ListTagsAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Future[Try[VersioningListTagsRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.repo_id" -> client.toQuery(repository_id_repo_id)
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    val body: String = null
    return client.request[String, VersioningListTagsRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags", __query, body, VersioningListTagsRequestResponse.fromJson)
  }

  def ListTags(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, repository_id_repo_id: String)(implicit ec: ExecutionContext): Try[VersioningListTagsRequestResponse] = Await.result(ListTagsAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, repository_id_repo_id), Duration.Inf)

  def ListTags2Async(repository_id_repo_id: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[VersioningListTagsRequestResponse]] = {
    val __query = Map[String,String](
      "repository_id.named_id.name" -> client.toQuery(repository_id_named_id_name),
      "repository_id.named_id.workspace_name" -> client.toQuery(repository_id_named_id_workspace_name)
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    val body: String = null
    return client.request[String, VersioningListTagsRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/tags", __query, body, VersioningListTagsRequestResponse.fromJson)
  }

  def ListTags2(repository_id_repo_id: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[VersioningListTagsRequestResponse] = Await.result(ListTags2Async(repository_id_repo_id, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def SetBranchAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, branch: String, body: String)(implicit ec: ExecutionContext): Future[Try[VersioningSetBranchRequestResponse]] = {
    val __query = Map[String,String](
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[String, VersioningSetBranchRequestResponse]("PUT", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/branches/$branch", __query, body, VersioningSetBranchRequestResponse.fromJson)
  }

  def SetBranch(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, branch: String, body: String)(implicit ec: ExecutionContext): Try[VersioningSetBranchRequestResponse] = Await.result(SetBranchAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, branch, body), Duration.Inf)

  def SetBranch2Async(repository_id_repo_id: String, branch: String, body: String)(implicit ec: ExecutionContext): Future[Try[VersioningSetBranchRequestResponse]] = {
    val __query = Map[String,String](
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (branch == null) throw new Exception("Missing required parameter \"branch\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[String, VersioningSetBranchRequestResponse]("PUT", basePath + s"/versioning/repositories/$repository_id_repo_id/branches/$branch", __query, body, VersioningSetBranchRequestResponse.fromJson)
  }

  def SetBranch2(repository_id_repo_id: String, branch: String, body: String)(implicit ec: ExecutionContext): Try[VersioningSetBranchRequestResponse] = Await.result(SetBranch2Async(repository_id_repo_id, branch, body), Duration.Inf)

  def SetTagAsync(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, tag: String, body: String)(implicit ec: ExecutionContext): Future[Try[VersioningSetTagRequestResponse]] = {
    val __query = Map[String,String](
    )
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[String, VersioningSetTagRequestResponse]("PUT", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags/$tag", __query, body, VersioningSetTagRequestResponse.fromJson)
  }

  def SetTag(repository_id_named_id_workspace_name: String, repository_id_named_id_name: String, tag: String, body: String)(implicit ec: ExecutionContext): Try[VersioningSetTagRequestResponse] = Await.result(SetTagAsync(repository_id_named_id_workspace_name, repository_id_named_id_name, tag, body), Duration.Inf)

  def SetTag2Async(repository_id_repo_id: String, tag: String, body: String)(implicit ec: ExecutionContext): Future[Try[VersioningSetTagRequestResponse]] = {
    val __query = Map[String,String](
    )
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (tag == null) throw new Exception("Missing required parameter \"tag\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[String, VersioningSetTagRequestResponse]("PUT", basePath + s"/versioning/repositories/$repository_id_repo_id/tags/$tag", __query, body, VersioningSetTagRequestResponse.fromJson)
  }

  def SetTag2(repository_id_repo_id: String, tag: String, body: String)(implicit ec: ExecutionContext): Try[VersioningSetTagRequestResponse] = Await.result(SetTag2Async(repository_id_repo_id, tag, body), Duration.Inf)

  def UpdateRepositoryAsync(id_named_id_workspace_name: String, id_named_id_name: String, body: VersioningRepository)(implicit ec: ExecutionContext): Future[Try[VersioningSetRepositoryResponse]] = {
    val __query = Map[String,String](
    )
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningRepository, VersioningSetRepositoryResponse]("PUT", basePath + s"/versioning/workspaces/$id_named_id_workspace_name/repositories/$id_named_id_name", __query, body, VersioningSetRepositoryResponse.fromJson)
  }

  def UpdateRepository(id_named_id_workspace_name: String, id_named_id_name: String, body: VersioningRepository)(implicit ec: ExecutionContext): Try[VersioningSetRepositoryResponse] = Await.result(UpdateRepositoryAsync(id_named_id_workspace_name, id_named_id_name, body), Duration.Inf)

  def UpdateRepository2Async(id_repo_id: String, body: VersioningRepository)(implicit ec: ExecutionContext): Future[Try[VersioningSetRepositoryResponse]] = {
    val __query = Map[String,String](
    )
    if (id_repo_id == null) throw new Exception("Missing required parameter \"id_repo_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningRepository, VersioningSetRepositoryResponse]("PUT", basePath + s"/versioning/repositories/$id_repo_id", __query, body, VersioningSetRepositoryResponse.fromJson)
  }

  def UpdateRepository2(id_repo_id: String, body: VersioningRepository)(implicit ec: ExecutionContext): Try[VersioningSetRepositoryResponse] = Await.result(UpdateRepository2Async(id_repo_id, body), Duration.Inf)

}
