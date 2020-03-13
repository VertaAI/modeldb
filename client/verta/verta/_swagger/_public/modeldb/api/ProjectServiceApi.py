# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class ProjectServiceApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def addProjectAttributes(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/addProjectAttributes"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddProjectAttributesResponse import ModeldbAddProjectAttributesResponse
      ret = ModeldbAddProjectAttributesResponse.from_json(ret)

    return ret

  def addProjectTag(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/addProjectTag"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddProjectTagResponse import ModeldbAddProjectTagResponse
      ret = ModeldbAddProjectTagResponse.from_json(ret)

    return ret

  def addProjectTags(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/addProjectTags"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddProjectTagsResponse import ModeldbAddProjectTagsResponse
      ret = ModeldbAddProjectTagsResponse.from_json(ret)

    return ret

  def createProject(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/createProject"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbCreateProjectResponse import ModeldbCreateProjectResponse
      ret = ModeldbCreateProjectResponse.from_json(ret)

    return ret

  def deepCopyProject(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/deepCopyProject"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeepCopyProjectResponse import ModeldbDeepCopyProjectResponse
      ret = ModeldbDeepCopyProjectResponse.from_json(ret)

    return ret

  def deleteArtifact(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/deleteArtifact"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteProjectArtifactResponse import ModeldbDeleteProjectArtifactResponse
      ret = ModeldbDeleteProjectArtifactResponse.from_json(ret)

    return ret

  def deleteProject(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/deleteProject"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteProjectResponse import ModeldbDeleteProjectResponse
      ret = ModeldbDeleteProjectResponse.from_json(ret)

    return ret

  def deleteProjectAttributes(self, id=None, attribute_keys=None, delete_all=None):
    __query = {
      "id": client.to_query(id),
      "attribute_keys": client.to_query(attribute_keys),
      "delete_all": client.to_query(delete_all)
    }
    body = None

    format_args = {}
    path = "/project/deleteProjectAttributes"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    if "$attribute_keys" in path:
      path = path.replace("$attribute_keys", "%(attribute_keys)s")
      format_args["attribute_keys"] = attribute_keys
    if "$delete_all" in path:
      path = path.replace("$delete_all", "%(delete_all)s")
      format_args["delete_all"] = delete_all
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteProjectAttributesResponse import ModeldbDeleteProjectAttributesResponse
      ret = ModeldbDeleteProjectAttributesResponse.from_json(ret)

    return ret

  def deleteProjectTag(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/deleteProjectTag"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteProjectTagResponse import ModeldbDeleteProjectTagResponse
      ret = ModeldbDeleteProjectTagResponse.from_json(ret)

    return ret

  def deleteProjectTags(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/deleteProjectTags"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteProjectTagsResponse import ModeldbDeleteProjectTagsResponse
      ret = ModeldbDeleteProjectTagsResponse.from_json(ret)

    return ret

  def deleteProjects(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/deleteProjects"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteProjectsResponse import ModeldbDeleteProjectsResponse
      ret = ModeldbDeleteProjectsResponse.from_json(ret)

    return ret

  def findProjects(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/findProjects"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbFindProjectsResponse import ModeldbFindProjectsResponse
      ret = ModeldbFindProjectsResponse.from_json(ret)

    return ret

  def getArtifacts(self, id=None, key=None):
    __query = {
      "id": client.to_query(id),
      "key": client.to_query(key)
    }
    body = None

    format_args = {}
    path = "/project/getArtifacts"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    if "$key" in path:
      path = path.replace("$key", "%(key)s")
      format_args["key"] = key
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetArtifactsResponse import ModeldbGetArtifactsResponse
      ret = ModeldbGetArtifactsResponse.from_json(ret)

    return ret

  def getProjectAttributes(self, id=None, attribute_keys=None, get_all=None):
    __query = {
      "id": client.to_query(id),
      "attribute_keys": client.to_query(attribute_keys),
      "get_all": client.to_query(get_all)
    }
    body = None

    format_args = {}
    path = "/project/getProjectAttributes"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    if "$attribute_keys" in path:
      path = path.replace("$attribute_keys", "%(attribute_keys)s")
      format_args["attribute_keys"] = attribute_keys
    if "$get_all" in path:
      path = path.replace("$get_all", "%(get_all)s")
      format_args["get_all"] = get_all
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetAttributesResponse import ModeldbGetAttributesResponse
      ret = ModeldbGetAttributesResponse.from_json(ret)

    return ret

  def getProjectById(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/project/getProjectById"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetProjectByIdResponse import ModeldbGetProjectByIdResponse
      ret = ModeldbGetProjectByIdResponse.from_json(ret)

    return ret

  def getProjectByName(self, name=None, workspace_name=None):
    __query = {
      "name": client.to_query(name),
      "workspace_name": client.to_query(workspace_name)
    }
    body = None

    format_args = {}
    path = "/project/getProjectByName"
    if "$name" in path:
      path = path.replace("$name", "%(name)s")
      format_args["name"] = name
    if "$workspace_name" in path:
      path = path.replace("$workspace_name", "%(workspace_name)s")
      format_args["workspace_name"] = workspace_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetProjectByNameResponse import ModeldbGetProjectByNameResponse
      ret = ModeldbGetProjectByNameResponse.from_json(ret)

    return ret

  def getProjectCodeVersion(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/project/getProjectCodeVersion"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetProjectCodeVersionResponse import ModeldbGetProjectCodeVersionResponse
      ret = ModeldbGetProjectCodeVersionResponse.from_json(ret)

    return ret

  def getProjectReadme(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/project/getProjectReadme"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetProjectReadmeResponse import ModeldbGetProjectReadmeResponse
      ret = ModeldbGetProjectReadmeResponse.from_json(ret)

    return ret

  def getProjectShortName(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/project/getProjectShortName"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetProjectShortNameResponse import ModeldbGetProjectShortNameResponse
      ret = ModeldbGetProjectShortNameResponse.from_json(ret)

    return ret

  def getProjectTags(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/project/getProjectTags"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetTagsResponse import ModeldbGetTagsResponse
      ret = ModeldbGetTagsResponse.from_json(ret)

    return ret

  def getProjects(self, page_number=None, page_limit=None, ascending=None, sort_key=None, workspace_name=None):
    __query = {
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key),
      "workspace_name": client.to_query(workspace_name)
    }
    body = None

    format_args = {}
    path = "/project/getProjects"
    if "$page_number" in path:
      path = path.replace("$page_number", "%(page_number)s")
      format_args["page_number"] = page_number
    if "$page_limit" in path:
      path = path.replace("$page_limit", "%(page_limit)s")
      format_args["page_limit"] = page_limit
    if "$ascending" in path:
      path = path.replace("$ascending", "%(ascending)s")
      format_args["ascending"] = ascending
    if "$sort_key" in path:
      path = path.replace("$sort_key", "%(sort_key)s")
      format_args["sort_key"] = sort_key
    if "$workspace_name" in path:
      path = path.replace("$workspace_name", "%(workspace_name)s")
      format_args["workspace_name"] = workspace_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetProjectsResponse import ModeldbGetProjectsResponse
      ret = ModeldbGetProjectsResponse.from_json(ret)

    return ret

  def getPublicProjects(self, user_id=None, workspace_name=None):
    __query = {
      "user_id": client.to_query(user_id),
      "workspace_name": client.to_query(workspace_name)
    }
    body = None

    format_args = {}
    path = "/project/getPublicProjects"
    if "$user_id" in path:
      path = path.replace("$user_id", "%(user_id)s")
      format_args["user_id"] = user_id
    if "$workspace_name" in path:
      path = path.replace("$workspace_name", "%(workspace_name)s")
      format_args["workspace_name"] = workspace_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetPublicProjectsResponse import ModeldbGetPublicProjectsResponse
      ret = ModeldbGetPublicProjectsResponse.from_json(ret)

    return ret

  def getSummary(self, entityId=None):
    __query = {
      "entityId": client.to_query(entityId)
    }
    body = None

    format_args = {}
    path = "/project/getSummary"
    if "$entityId" in path:
      path = path.replace("$entityId", "%(entityId)s")
      format_args["entityId"] = entityId
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetSummaryResponse import ModeldbGetSummaryResponse
      ret = ModeldbGetSummaryResponse.from_json(ret)

    return ret

  def getUrlForArtifact(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/getUrlForArtifact"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetUrlForArtifactResponse import ModeldbGetUrlForArtifactResponse
      ret = ModeldbGetUrlForArtifactResponse.from_json(ret)

    return ret

  def logArtifacts(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/logArtifacts"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogProjectArtifactsResponse import ModeldbLogProjectArtifactsResponse
      ret = ModeldbLogProjectArtifactsResponse.from_json(ret)

    return ret

  def logProjectCodeVersion(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/logProjectCodeVersion"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogProjectCodeVersionResponse import ModeldbLogProjectCodeVersionResponse
      ret = ModeldbLogProjectCodeVersionResponse.from_json(ret)

    return ret

  def setProjectReadme(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/setProjectReadme"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbSetProjectReadmeResponse import ModeldbSetProjectReadmeResponse
      ret = ModeldbSetProjectReadmeResponse.from_json(ret)

    return ret

  def setProjectShortName(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/setProjectShortName"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbSetProjectShortNameResponse import ModeldbSetProjectShortNameResponse
      ret = ModeldbSetProjectShortNameResponse.from_json(ret)

    return ret

  def setProjectVisibility(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/setProjectVisibility"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbSetProjectVisibiltyResponse import ModeldbSetProjectVisibiltyResponse
      ret = ModeldbSetProjectVisibiltyResponse.from_json(ret)

    return ret

  def setProjectWorkspace(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/setProjectWorkspace"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbSetProjectWorkspaceResponse import ModeldbSetProjectWorkspaceResponse
      ret = ModeldbSetProjectWorkspaceResponse.from_json(ret)

    return ret

  def updateProjectAttributes(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/updateProjectAttributes"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateProjectAttributesResponse import ModeldbUpdateProjectAttributesResponse
      ret = ModeldbUpdateProjectAttributesResponse.from_json(ret)

    return ret

  def updateProjectDescription(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/updateProjectDescription"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateProjectDescriptionResponse import ModeldbUpdateProjectDescriptionResponse
      ret = ModeldbUpdateProjectDescriptionResponse.from_json(ret)

    return ret

  def updateProjectName(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/project/updateProjectName"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateProjectNameResponse import ModeldbUpdateProjectNameResponse
      ret = ModeldbUpdateProjectNameResponse.from_json(ret)

    return ret

  def verifyConnection(self, ):
    __query = {
    }
    body = None

    format_args = {}
    path = "/project/verifyConnection"
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbVerifyConnectionResponse import ModeldbVerifyConnectionResponse
      ret = ModeldbVerifyConnectionResponse.from_json(ret)

    return ret
