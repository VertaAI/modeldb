# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class ExperimentServiceApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def addAttribute(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/addAttribute"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddAttributesResponse import ModeldbAddAttributesResponse
      ret = ModeldbAddAttributesResponse.from_json(ret)

    return ret

  def addExperimentAttributes(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/addExperimentAttributes"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddExperimentAttributesResponse import ModeldbAddExperimentAttributesResponse
      ret = ModeldbAddExperimentAttributesResponse.from_json(ret)

    return ret

  def addExperimentTag(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/addExperimentTag"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddExperimentTagResponse import ModeldbAddExperimentTagResponse
      ret = ModeldbAddExperimentTagResponse.from_json(ret)

    return ret

  def addExperimentTags(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/addExperimentTags"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddExperimentTagsResponse import ModeldbAddExperimentTagsResponse
      ret = ModeldbAddExperimentTagsResponse.from_json(ret)

    return ret

  def createExperiment(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/createExperiment"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbCreateExperimentResponse import ModeldbCreateExperimentResponse
      ret = ModeldbCreateExperimentResponse.from_json(ret)

    return ret

  def deleteArtifact(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/deleteArtifact"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteExperimentArtifactResponse import ModeldbDeleteExperimentArtifactResponse
      ret = ModeldbDeleteExperimentArtifactResponse.from_json(ret)

    return ret

  def deleteExperiment(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/deleteExperiment"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteExperimentResponse import ModeldbDeleteExperimentResponse
      ret = ModeldbDeleteExperimentResponse.from_json(ret)

    return ret

  def deleteExperimentAttributes(self, id=None, attribute_keys=None, delete_all=None):
    __query = {
      "id": client.to_query(id),
      "attribute_keys": client.to_query(attribute_keys),
      "delete_all": client.to_query(delete_all)
    }
    body = None

    format_args = {}
    path = "/experiment/deleteExperimentAttributes"
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
      from ..model.ModeldbDeleteExperimentAttributesResponse import ModeldbDeleteExperimentAttributesResponse
      ret = ModeldbDeleteExperimentAttributesResponse.from_json(ret)

    return ret

  def deleteExperimentTag(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/deleteExperimentTag"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteExperimentTagResponse import ModeldbDeleteExperimentTagResponse
      ret = ModeldbDeleteExperimentTagResponse.from_json(ret)

    return ret

  def deleteExperimentTags(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/deleteExperimentTags"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteExperimentTagsResponse import ModeldbDeleteExperimentTagsResponse
      ret = ModeldbDeleteExperimentTagsResponse.from_json(ret)

    return ret

  def deleteExperiments(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/deleteExperiments"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteExperimentsResponse import ModeldbDeleteExperimentsResponse
      ret = ModeldbDeleteExperimentsResponse.from_json(ret)

    return ret

  def findExperiments(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/findExperiments"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbFindExperimentsResponse import ModeldbFindExperimentsResponse
      ret = ModeldbFindExperimentsResponse.from_json(ret)

    return ret

  def getArtifacts(self, id=None, key=None):
    __query = {
      "id": client.to_query(id),
      "key": client.to_query(key)
    }
    body = None

    format_args = {}
    path = "/experiment/getArtifacts"
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

  def getExperimentAttributes(self, id=None, attribute_keys=None, get_all=None):
    __query = {
      "id": client.to_query(id),
      "attribute_keys": client.to_query(attribute_keys),
      "get_all": client.to_query(get_all)
    }
    body = None

    format_args = {}
    path = "/experiment/getExperimentAttributes"
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

  def getExperimentById(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/experiment/getExperimentById"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetExperimentByIdResponse import ModeldbGetExperimentByIdResponse
      ret = ModeldbGetExperimentByIdResponse.from_json(ret)

    return ret

  def getExperimentByName(self, name=None, project_id=None):
    __query = {
      "name": client.to_query(name),
      "project_id": client.to_query(project_id)
    }
    body = None

    format_args = {}
    path = "/experiment/getExperimentByName"
    if "$name" in path:
      path = path.replace("$name", "%(name)s")
      format_args["name"] = name
    if "$project_id" in path:
      path = path.replace("$project_id", "%(project_id)s")
      format_args["project_id"] = project_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetExperimentByNameResponse import ModeldbGetExperimentByNameResponse
      ret = ModeldbGetExperimentByNameResponse.from_json(ret)

    return ret

  def getExperimentCodeVersion(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/experiment/getExperimentCodeVersion"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetExperimentCodeVersionResponse import ModeldbGetExperimentCodeVersionResponse
      ret = ModeldbGetExperimentCodeVersionResponse.from_json(ret)

    return ret

  def getExperimentTags(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/experiment/getExperimentTags"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetTagsResponse import ModeldbGetTagsResponse
      ret = ModeldbGetTagsResponse.from_json(ret)

    return ret

  def getExperimentsInProject(self, project_id=None, page_number=None, page_limit=None, ascending=None, sort_key=None):
    __query = {
      "project_id": client.to_query(project_id),
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key)
    }
    body = None

    format_args = {}
    path = "/experiment/getExperimentsInProject"
    if "$project_id" in path:
      path = path.replace("$project_id", "%(project_id)s")
      format_args["project_id"] = project_id
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
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetExperimentsInProjectResponse import ModeldbGetExperimentsInProjectResponse
      ret = ModeldbGetExperimentsInProjectResponse.from_json(ret)

    return ret

  def getUrlForArtifact(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/getUrlForArtifact"
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
    path = "/experiment/logArtifacts"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogExperimentArtifactsResponse import ModeldbLogExperimentArtifactsResponse
      ret = ModeldbLogExperimentArtifactsResponse.from_json(ret)

    return ret

  def logExperimentCodeVersion(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/logExperimentCodeVersion"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogExperimentCodeVersionResponse import ModeldbLogExperimentCodeVersionResponse
      ret = ModeldbLogExperimentCodeVersionResponse.from_json(ret)

    return ret

  def updateExperimentDescription(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/updateExperimentDescription"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateExperimentDescriptionResponse import ModeldbUpdateExperimentDescriptionResponse
      ret = ModeldbUpdateExperimentDescriptionResponse.from_json(ret)

    return ret

  def updateExperimentName(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/updateExperimentName"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateExperimentNameResponse import ModeldbUpdateExperimentNameResponse
      ret = ModeldbUpdateExperimentNameResponse.from_json(ret)

    return ret

  def updateExperimentNameOrDescription(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment/updateExperimentNameOrDescription"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateExperimentNameOrDescriptionResponse import ModeldbUpdateExperimentNameOrDescriptionResponse
      ret = ModeldbUpdateExperimentNameOrDescriptionResponse.from_json(ret)

    return ret
