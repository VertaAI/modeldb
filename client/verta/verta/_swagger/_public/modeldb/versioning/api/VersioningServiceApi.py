# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class VersioningServiceApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def ComputeRepositoryDiff(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, repository_id_repo_id=None, commit_a=None, commit_b=None, path_prefix=None):
    __query = {
      "repository_id.repo_id": client.to_query(repository_id_repo_id),
      "commit_a": client.to_query(commit_a),
      "commit_b": client.to_query(commit_b),
      "path_prefix": client.to_query(path_prefix)
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/diff"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$commit_a" in path:
      path = path.replace("$commit_a", "%(commit_a)s")
      format_args["commit_a"] = commit_a
    if "$commit_b" in path:
      path = path.replace("$commit_b", "%(commit_b)s")
      format_args["commit_b"] = commit_b
    if "$path_prefix" in path:
      path = path.replace("$path_prefix", "%(path_prefix)s")
      format_args["path_prefix"] = path_prefix
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningComputeRepositoryDiffRequestResponse import VersioningComputeRepositoryDiffRequestResponse
      ret = VersioningComputeRepositoryDiffRequestResponse.from_json(ret)

    return ret

  def ComputeRepositoryDiff2(self, repository_id_repo_id=None, repository_id_named_id_name=None, repository_id_named_id_workspace_name=None, commit_a=None, commit_b=None, path_prefix=None):
    __query = {
      "repository_id.named_id.name": client.to_query(repository_id_named_id_name),
      "repository_id.named_id.workspace_name": client.to_query(repository_id_named_id_workspace_name),
      "commit_a": client.to_query(commit_a),
      "commit_b": client.to_query(commit_b),
      "path_prefix": client.to_query(path_prefix)
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/diff"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$commit_a" in path:
      path = path.replace("$commit_a", "%(commit_a)s")
      format_args["commit_a"] = commit_a
    if "$commit_b" in path:
      path = path.replace("$commit_b", "%(commit_b)s")
      format_args["commit_b"] = commit_b
    if "$path_prefix" in path:
      path = path.replace("$path_prefix", "%(path_prefix)s")
      format_args["path_prefix"] = path_prefix
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningComputeRepositoryDiffRequestResponse import VersioningComputeRepositoryDiffRequestResponse
      ret = VersioningComputeRepositoryDiffRequestResponse.from_json(ret)

    return ret

  def CreateCommit(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, body=None):
    __query = {
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningCreateCommitRequestResponse import VersioningCreateCommitRequestResponse
      ret = VersioningCreateCommitRequestResponse.from_json(ret)

    return ret

  def CreateCommit2(self, repository_id_repo_id=None, body=None):
    __query = {
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/commits"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("PUT", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningCreateCommitRequestResponse import VersioningCreateCommitRequestResponse
      ret = VersioningCreateCommitRequestResponse.from_json(ret)

    return ret

  def CreateRepository(self, id_named_id_workspace_name=None, body=None):
    __query = {
    }
    if id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/versioning/workspaces/$id_named_id_workspace_name/repositories"
    if "$id_named_id_workspace_name" in path:
      path = path.replace("$id_named_id_workspace_name", "%(id_named_id_workspace_name)s")
      format_args["id_named_id_workspace_name"] = id_named_id_workspace_name
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningSetRepositoryResponse import VersioningSetRepositoryResponse
      ret = VersioningSetRepositoryResponse.from_json(ret)

    return ret

  def DeleteCommit(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, commit_sha=None, repository_id_repo_id=None):
    __query = {
      "repository_id.repo_id": client.to_query(repository_id_repo_id)
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    if commit_sha is None:
      raise Exception("Missing required parameter \"commit_sha\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$commit_sha" in path:
      path = path.replace("$commit_sha", "%(commit_sha)s")
      format_args["commit_sha"] = commit_sha
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningDeleteCommitRequestResponse import VersioningDeleteCommitRequestResponse
      ret = VersioningDeleteCommitRequestResponse.from_json(ret)

    return ret

  def DeleteCommit2(self, repository_id_repo_id=None, commit_sha=None, repository_id_named_id_name=None, repository_id_named_id_workspace_name=None):
    __query = {
      "repository_id.named_id.name": client.to_query(repository_id_named_id_name),
      "repository_id.named_id.workspace_name": client.to_query(repository_id_named_id_workspace_name)
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    if commit_sha is None:
      raise Exception("Missing required parameter \"commit_sha\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/commits/$commit_sha"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$commit_sha" in path:
      path = path.replace("$commit_sha", "%(commit_sha)s")
      format_args["commit_sha"] = commit_sha
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningDeleteCommitRequestResponse import VersioningDeleteCommitRequestResponse
      ret = VersioningDeleteCommitRequestResponse.from_json(ret)

    return ret

  def DeleteRepository(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, repository_id_repo_id=None):
    __query = {
      "repository_id.repo_id": client.to_query(repository_id_repo_id)
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningDeleteRepositoryRequestResponse import VersioningDeleteRepositoryRequestResponse
      ret = VersioningDeleteRepositoryRequestResponse.from_json(ret)

    return ret

  def DeleteRepository2(self, repository_id_repo_id=None, repository_id_named_id_name=None, repository_id_named_id_workspace_name=None):
    __query = {
      "repository_id.named_id.name": client.to_query(repository_id_named_id_name),
      "repository_id.named_id.workspace_name": client.to_query(repository_id_named_id_workspace_name)
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningDeleteRepositoryRequestResponse import VersioningDeleteRepositoryRequestResponse
      ret = VersioningDeleteRepositoryRequestResponse.from_json(ret)

    return ret

  def DeleteTag(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, tag=None, repository_id_repo_id=None):
    __query = {
      "repository_id.repo_id": client.to_query(repository_id_repo_id)
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    if tag is None:
      raise Exception("Missing required parameter \"tag\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags/$tag"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$tag" in path:
      path = path.replace("$tag", "%(tag)s")
      format_args["tag"] = tag
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningDeleteTagRequestResponse import VersioningDeleteTagRequestResponse
      ret = VersioningDeleteTagRequestResponse.from_json(ret)

    return ret

  def DeleteTag2(self, repository_id_repo_id=None, tag=None, repository_id_named_id_name=None, repository_id_named_id_workspace_name=None):
    __query = {
      "repository_id.named_id.name": client.to_query(repository_id_named_id_name),
      "repository_id.named_id.workspace_name": client.to_query(repository_id_named_id_workspace_name)
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    if tag is None:
      raise Exception("Missing required parameter \"tag\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/tags/$tag"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$tag" in path:
      path = path.replace("$tag", "%(tag)s")
      format_args["tag"] = tag
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningDeleteTagRequestResponse import VersioningDeleteTagRequestResponse
      ret = VersioningDeleteTagRequestResponse.from_json(ret)

    return ret

  def GetCommit(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, commit_sha=None, repository_id_repo_id=None):
    __query = {
      "repository_id.repo_id": client.to_query(repository_id_repo_id)
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    if commit_sha is None:
      raise Exception("Missing required parameter \"commit_sha\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$commit_sha" in path:
      path = path.replace("$commit_sha", "%(commit_sha)s")
      format_args["commit_sha"] = commit_sha
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningGetCommitRequestResponse import VersioningGetCommitRequestResponse
      ret = VersioningGetCommitRequestResponse.from_json(ret)

    return ret

  def GetCommit2(self, repository_id_repo_id=None, commit_sha=None, repository_id_named_id_name=None, repository_id_named_id_workspace_name=None):
    __query = {
      "repository_id.named_id.name": client.to_query(repository_id_named_id_name),
      "repository_id.named_id.workspace_name": client.to_query(repository_id_named_id_workspace_name)
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    if commit_sha is None:
      raise Exception("Missing required parameter \"commit_sha\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/commits/$commit_sha"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$commit_sha" in path:
      path = path.replace("$commit_sha", "%(commit_sha)s")
      format_args["commit_sha"] = commit_sha
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningGetCommitRequestResponse import VersioningGetCommitRequestResponse
      ret = VersioningGetCommitRequestResponse.from_json(ret)

    return ret

  def GetCommitBlob(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, commit_sha=None, repository_id_repo_id=None, path=None):
    __query = {
      "repository_id.repo_id": client.to_query(repository_id_repo_id),
      "path": client.to_query(path)
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    if commit_sha is None:
      raise Exception("Missing required parameter \"commit_sha\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/blobs/path"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$commit_sha" in path:
      path = path.replace("$commit_sha", "%(commit_sha)s")
      format_args["commit_sha"] = commit_sha
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$path" in path:
      path = path.replace("$path", "%(path)s")
      format_args["path"] = path
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningGetCommitBlobRequestResponse import VersioningGetCommitBlobRequestResponse
      ret = VersioningGetCommitBlobRequestResponse.from_json(ret)

    return ret

  def GetCommitBlob2(self, repository_id_repo_id=None, commit_sha=None, repository_id_named_id_name=None, repository_id_named_id_workspace_name=None, path=None):
    __query = {
      "repository_id.named_id.name": client.to_query(repository_id_named_id_name),
      "repository_id.named_id.workspace_name": client.to_query(repository_id_named_id_workspace_name),
      "path": client.to_query(path)
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    if commit_sha is None:
      raise Exception("Missing required parameter \"commit_sha\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/blobs/path"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$commit_sha" in path:
      path = path.replace("$commit_sha", "%(commit_sha)s")
      format_args["commit_sha"] = commit_sha
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$path" in path:
      path = path.replace("$path", "%(path)s")
      format_args["path"] = path
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningGetCommitBlobRequestResponse import VersioningGetCommitBlobRequestResponse
      ret = VersioningGetCommitBlobRequestResponse.from_json(ret)

    return ret

  def GetCommitFolder(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, commit_sha=None, repository_id_repo_id=None, path=None):
    __query = {
      "repository_id.repo_id": client.to_query(repository_id_repo_id),
      "path": client.to_query(path)
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    if commit_sha is None:
      raise Exception("Missing required parameter \"commit_sha\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/folders/path"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$commit_sha" in path:
      path = path.replace("$commit_sha", "%(commit_sha)s")
      format_args["commit_sha"] = commit_sha
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$path" in path:
      path = path.replace("$path", "%(path)s")
      format_args["path"] = path
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningGetCommitFolderRequestResponse import VersioningGetCommitFolderRequestResponse
      ret = VersioningGetCommitFolderRequestResponse.from_json(ret)

    return ret

  def GetCommitFolder2(self, repository_id_repo_id=None, commit_sha=None, repository_id_named_id_name=None, repository_id_named_id_workspace_name=None, path=None):
    __query = {
      "repository_id.named_id.name": client.to_query(repository_id_named_id_name),
      "repository_id.named_id.workspace_name": client.to_query(repository_id_named_id_workspace_name),
      "path": client.to_query(path)
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    if commit_sha is None:
      raise Exception("Missing required parameter \"commit_sha\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/folders/path"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$commit_sha" in path:
      path = path.replace("$commit_sha", "%(commit_sha)s")
      format_args["commit_sha"] = commit_sha
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$path" in path:
      path = path.replace("$path", "%(path)s")
      format_args["path"] = path
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningGetCommitFolderRequestResponse import VersioningGetCommitFolderRequestResponse
      ret = VersioningGetCommitFolderRequestResponse.from_json(ret)

    return ret

  def GetRepository(self, id_named_id_workspace_name=None, id_named_id_name=None, id_repo_id=None):
    __query = {
      "id.repo_id": client.to_query(id_repo_id)
    }
    if id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if id_named_id_name is None:
      raise Exception("Missing required parameter \"id_named_id_name\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$id_named_id_workspace_name/repositories/$id_named_id_name"
    if "$id_named_id_workspace_name" in path:
      path = path.replace("$id_named_id_workspace_name", "%(id_named_id_workspace_name)s")
      format_args["id_named_id_workspace_name"] = id_named_id_workspace_name
    if "$id_named_id_name" in path:
      path = path.replace("$id_named_id_name", "%(id_named_id_name)s")
      format_args["id_named_id_name"] = id_named_id_name
    if "$id_repo_id" in path:
      path = path.replace("$id_repo_id", "%(id_repo_id)s")
      format_args["id_repo_id"] = id_repo_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningGetRepositoryRequestResponse import VersioningGetRepositoryRequestResponse
      ret = VersioningGetRepositoryRequestResponse.from_json(ret)

    return ret

  def GetRepository2(self, id_repo_id=None, id_named_id_name=None, id_named_id_workspace_name=None):
    __query = {
      "id.named_id.name": client.to_query(id_named_id_name),
      "id.named_id.workspace_name": client.to_query(id_named_id_workspace_name)
    }
    if id_repo_id is None:
      raise Exception("Missing required parameter \"id_repo_id\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$id_repo_id"
    if "$id_repo_id" in path:
      path = path.replace("$id_repo_id", "%(id_repo_id)s")
      format_args["id_repo_id"] = id_repo_id
    if "$id_named_id_name" in path:
      path = path.replace("$id_named_id_name", "%(id_named_id_name)s")
      format_args["id_named_id_name"] = id_named_id_name
    if "$id_named_id_workspace_name" in path:
      path = path.replace("$id_named_id_workspace_name", "%(id_named_id_workspace_name)s")
      format_args["id_named_id_workspace_name"] = id_named_id_workspace_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningGetRepositoryRequestResponse import VersioningGetRepositoryRequestResponse
      ret = VersioningGetRepositoryRequestResponse.from_json(ret)

    return ret

  def GetTag(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, tag=None, repository_id_repo_id=None):
    __query = {
      "repository_id.repo_id": client.to_query(repository_id_repo_id)
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    if tag is None:
      raise Exception("Missing required parameter \"tag\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags/$tag"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$tag" in path:
      path = path.replace("$tag", "%(tag)s")
      format_args["tag"] = tag
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningGetTagRequestResponse import VersioningGetTagRequestResponse
      ret = VersioningGetTagRequestResponse.from_json(ret)

    return ret

  def GetTag2(self, repository_id_repo_id=None, tag=None, repository_id_named_id_name=None, repository_id_named_id_workspace_name=None):
    __query = {
      "repository_id.named_id.name": client.to_query(repository_id_named_id_name),
      "repository_id.named_id.workspace_name": client.to_query(repository_id_named_id_workspace_name)
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    if tag is None:
      raise Exception("Missing required parameter \"tag\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/tags/$tag"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$tag" in path:
      path = path.replace("$tag", "%(tag)s")
      format_args["tag"] = tag
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningGetTagRequestResponse import VersioningGetTagRequestResponse
      ret = VersioningGetTagRequestResponse.from_json(ret)

    return ret

  def ListCommitBlobs(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, commit_sha=None, repository_id_repo_id=None, pagination_page_number=None, pagination_page_limit=None, path_prefix=None):
    __query = {
      "repository_id.repo_id": client.to_query(repository_id_repo_id),
      "pagination.page_number": client.to_query(pagination_page_number),
      "pagination.page_limit": client.to_query(pagination_page_limit),
      "path_prefix": client.to_query(path_prefix)
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    if commit_sha is None:
      raise Exception("Missing required parameter \"commit_sha\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/blobs"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$commit_sha" in path:
      path = path.replace("$commit_sha", "%(commit_sha)s")
      format_args["commit_sha"] = commit_sha
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$pagination_page_number" in path:
      path = path.replace("$pagination_page_number", "%(pagination_page_number)s")
      format_args["pagination_page_number"] = pagination_page_number
    if "$pagination_page_limit" in path:
      path = path.replace("$pagination_page_limit", "%(pagination_page_limit)s")
      format_args["pagination_page_limit"] = pagination_page_limit
    if "$path_prefix" in path:
      path = path.replace("$path_prefix", "%(path_prefix)s")
      format_args["path_prefix"] = path_prefix
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningListCommitBlobsRequestResponse import VersioningListCommitBlobsRequestResponse
      ret = VersioningListCommitBlobsRequestResponse.from_json(ret)

    return ret

  def ListCommitBlobs2(self, repository_id_repo_id=None, commit_sha=None, repository_id_named_id_name=None, repository_id_named_id_workspace_name=None, pagination_page_number=None, pagination_page_limit=None, path_prefix=None):
    __query = {
      "repository_id.named_id.name": client.to_query(repository_id_named_id_name),
      "repository_id.named_id.workspace_name": client.to_query(repository_id_named_id_workspace_name),
      "pagination.page_number": client.to_query(pagination_page_number),
      "pagination.page_limit": client.to_query(pagination_page_limit),
      "path_prefix": client.to_query(path_prefix)
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    if commit_sha is None:
      raise Exception("Missing required parameter \"commit_sha\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/blobs"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$commit_sha" in path:
      path = path.replace("$commit_sha", "%(commit_sha)s")
      format_args["commit_sha"] = commit_sha
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$pagination_page_number" in path:
      path = path.replace("$pagination_page_number", "%(pagination_page_number)s")
      format_args["pagination_page_number"] = pagination_page_number
    if "$pagination_page_limit" in path:
      path = path.replace("$pagination_page_limit", "%(pagination_page_limit)s")
      format_args["pagination_page_limit"] = pagination_page_limit
    if "$path_prefix" in path:
      path = path.replace("$path_prefix", "%(path_prefix)s")
      format_args["path_prefix"] = path_prefix
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningListCommitBlobsRequestResponse import VersioningListCommitBlobsRequestResponse
      ret = VersioningListCommitBlobsRequestResponse.from_json(ret)

    return ret

  def ListCommits(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, repository_id_repo_id=None, pagination_page_number=None, pagination_page_limit=None, commit_base=None, commit_head=None, path_prefix=None):
    __query = {
      "repository_id.repo_id": client.to_query(repository_id_repo_id),
      "pagination.page_number": client.to_query(pagination_page_number),
      "pagination.page_limit": client.to_query(pagination_page_limit),
      "commit_base": client.to_query(commit_base),
      "commit_head": client.to_query(commit_head),
      "path_prefix": client.to_query(path_prefix)
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$pagination_page_number" in path:
      path = path.replace("$pagination_page_number", "%(pagination_page_number)s")
      format_args["pagination_page_number"] = pagination_page_number
    if "$pagination_page_limit" in path:
      path = path.replace("$pagination_page_limit", "%(pagination_page_limit)s")
      format_args["pagination_page_limit"] = pagination_page_limit
    if "$commit_base" in path:
      path = path.replace("$commit_base", "%(commit_base)s")
      format_args["commit_base"] = commit_base
    if "$commit_head" in path:
      path = path.replace("$commit_head", "%(commit_head)s")
      format_args["commit_head"] = commit_head
    if "$path_prefix" in path:
      path = path.replace("$path_prefix", "%(path_prefix)s")
      format_args["path_prefix"] = path_prefix
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningListCommitsRequestResponse import VersioningListCommitsRequestResponse
      ret = VersioningListCommitsRequestResponse.from_json(ret)

    return ret

  def ListCommits2(self, repository_id_repo_id=None, repository_id_named_id_name=None, repository_id_named_id_workspace_name=None, pagination_page_number=None, pagination_page_limit=None, commit_base=None, commit_head=None, path_prefix=None):
    __query = {
      "repository_id.named_id.name": client.to_query(repository_id_named_id_name),
      "repository_id.named_id.workspace_name": client.to_query(repository_id_named_id_workspace_name),
      "pagination.page_number": client.to_query(pagination_page_number),
      "pagination.page_limit": client.to_query(pagination_page_limit),
      "commit_base": client.to_query(commit_base),
      "commit_head": client.to_query(commit_head),
      "path_prefix": client.to_query(path_prefix)
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/commits"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$pagination_page_number" in path:
      path = path.replace("$pagination_page_number", "%(pagination_page_number)s")
      format_args["pagination_page_number"] = pagination_page_number
    if "$pagination_page_limit" in path:
      path = path.replace("$pagination_page_limit", "%(pagination_page_limit)s")
      format_args["pagination_page_limit"] = pagination_page_limit
    if "$commit_base" in path:
      path = path.replace("$commit_base", "%(commit_base)s")
      format_args["commit_base"] = commit_base
    if "$commit_head" in path:
      path = path.replace("$commit_head", "%(commit_head)s")
      format_args["commit_head"] = commit_head
    if "$path_prefix" in path:
      path = path.replace("$path_prefix", "%(path_prefix)s")
      format_args["path_prefix"] = path_prefix
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningListCommitsRequestResponse import VersioningListCommitsRequestResponse
      ret = VersioningListCommitsRequestResponse.from_json(ret)

    return ret

  def ListRepositories(self, workspace_name=None, pagination_page_number=None, pagination_page_limit=None):
    __query = {
      "pagination.page_number": client.to_query(pagination_page_number),
      "pagination.page_limit": client.to_query(pagination_page_limit)
    }
    if workspace_name is None:
      raise Exception("Missing required parameter \"workspace_name\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$workspace_name/repositories"
    if "$workspace_name" in path:
      path = path.replace("$workspace_name", "%(workspace_name)s")
      format_args["workspace_name"] = workspace_name
    if "$pagination_page_number" in path:
      path = path.replace("$pagination_page_number", "%(pagination_page_number)s")
      format_args["pagination_page_number"] = pagination_page_number
    if "$pagination_page_limit" in path:
      path = path.replace("$pagination_page_limit", "%(pagination_page_limit)s")
      format_args["pagination_page_limit"] = pagination_page_limit
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningListRepositoriesRequestResponse import VersioningListRepositoriesRequestResponse
      ret = VersioningListRepositoriesRequestResponse.from_json(ret)

    return ret

  def ListRepositories2(self, workspace_name=None, pagination_page_number=None, pagination_page_limit=None):
    __query = {
      "workspace_name": client.to_query(workspace_name),
      "pagination.page_number": client.to_query(pagination_page_number),
      "pagination.page_limit": client.to_query(pagination_page_limit)
    }
    body = None

    format_args = {}
    path = "/versioning/repositories"
    if "$workspace_name" in path:
      path = path.replace("$workspace_name", "%(workspace_name)s")
      format_args["workspace_name"] = workspace_name
    if "$pagination_page_number" in path:
      path = path.replace("$pagination_page_number", "%(pagination_page_number)s")
      format_args["pagination_page_number"] = pagination_page_number
    if "$pagination_page_limit" in path:
      path = path.replace("$pagination_page_limit", "%(pagination_page_limit)s")
      format_args["pagination_page_limit"] = pagination_page_limit
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningListRepositoriesRequestResponse import VersioningListRepositoriesRequestResponse
      ret = VersioningListRepositoriesRequestResponse.from_json(ret)

    return ret

  def ListTags(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, repository_id_repo_id=None):
    __query = {
      "repository_id.repo_id": client.to_query(repository_id_repo_id)
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    body = None

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningListTagsRequestResponse import VersioningListTagsRequestResponse
      ret = VersioningListTagsRequestResponse.from_json(ret)

    return ret

  def ListTags2(self, repository_id_repo_id=None, repository_id_named_id_name=None, repository_id_named_id_workspace_name=None):
    __query = {
      "repository_id.named_id.name": client.to_query(repository_id_named_id_name),
      "repository_id.named_id.workspace_name": client.to_query(repository_id_named_id_workspace_name)
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    body = None

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/tags"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningListTagsRequestResponse import VersioningListTagsRequestResponse
      ret = VersioningListTagsRequestResponse.from_json(ret)

    return ret

  def SetTag(self, repository_id_named_id_workspace_name=None, repository_id_named_id_name=None, tag=None, body=None):
    __query = {
    }
    if repository_id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if repository_id_named_id_name is None:
      raise Exception("Missing required parameter \"repository_id_named_id_name\"")
    if tag is None:
      raise Exception("Missing required parameter \"tag\"")
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/tags/$tag"
    if "$repository_id_named_id_workspace_name" in path:
      path = path.replace("$repository_id_named_id_workspace_name", "%(repository_id_named_id_workspace_name)s")
      format_args["repository_id_named_id_workspace_name"] = repository_id_named_id_workspace_name
    if "$repository_id_named_id_name" in path:
      path = path.replace("$repository_id_named_id_name", "%(repository_id_named_id_name)s")
      format_args["repository_id_named_id_name"] = repository_id_named_id_name
    if "$tag" in path:
      path = path.replace("$tag", "%(tag)s")
      format_args["tag"] = tag
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("PUT", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningSetTagRequestResponse import VersioningSetTagRequestResponse
      ret = VersioningSetTagRequestResponse.from_json(ret)

    return ret

  def SetTag2(self, repository_id_repo_id=None, tag=None, body=None):
    __query = {
    }
    if repository_id_repo_id is None:
      raise Exception("Missing required parameter \"repository_id_repo_id\"")
    if tag is None:
      raise Exception("Missing required parameter \"tag\"")
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/versioning/repositories/$repository_id_repo_id/tags/$tag"
    if "$repository_id_repo_id" in path:
      path = path.replace("$repository_id_repo_id", "%(repository_id_repo_id)s")
      format_args["repository_id_repo_id"] = repository_id_repo_id
    if "$tag" in path:
      path = path.replace("$tag", "%(tag)s")
      format_args["tag"] = tag
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("PUT", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningSetTagRequestResponse import VersioningSetTagRequestResponse
      ret = VersioningSetTagRequestResponse.from_json(ret)

    return ret

  def UpdateRepository(self, id_named_id_workspace_name=None, id_named_id_name=None, body=None):
    __query = {
    }
    if id_named_id_workspace_name is None:
      raise Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if id_named_id_name is None:
      raise Exception("Missing required parameter \"id_named_id_name\"")
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/versioning/workspaces/$id_named_id_workspace_name/repositories/$id_named_id_name"
    if "$id_named_id_workspace_name" in path:
      path = path.replace("$id_named_id_workspace_name", "%(id_named_id_workspace_name)s")
      format_args["id_named_id_workspace_name"] = id_named_id_workspace_name
    if "$id_named_id_name" in path:
      path = path.replace("$id_named_id_name", "%(id_named_id_name)s")
      format_args["id_named_id_name"] = id_named_id_name
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("PUT", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningSetRepositoryResponse import VersioningSetRepositoryResponse
      ret = VersioningSetRepositoryResponse.from_json(ret)

    return ret

  def UpdateRepository2(self, id_repo_id=None, body=None):
    __query = {
    }
    if id_repo_id is None:
      raise Exception("Missing required parameter \"id_repo_id\"")
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/versioning/repositories/$id_repo_id"
    if "$id_repo_id" in path:
      path = path.replace("$id_repo_id", "%(id_repo_id)s")
      format_args["id_repo_id"] = id_repo_id
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("PUT", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.VersioningSetRepositoryResponse import VersioningSetRepositoryResponse
      ret = VersioningSetRepositoryResponse.from_json(ret)

    return ret
