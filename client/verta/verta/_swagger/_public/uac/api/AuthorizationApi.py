# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class AuthorizationApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def getAllowedEntities(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/authz/getAllowedEntities"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetAllowedEntitiesResponse import UacGetAllowedEntitiesResponse
      ret = UacGetAllowedEntitiesResponse.from_json(ret)

    return ret

  def getAllowedResources(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/authz/getAllowedResources"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetAllowedResourcesResponse import UacGetAllowedResourcesResponse
      ret = UacGetAllowedResourcesResponse.from_json(ret)

    return ret

  def getDireclyAllowedResources(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/authz/getDirectlyAllowedResources"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetAllowedResourcesResponse import UacGetAllowedResourcesResponse
      ret = UacGetAllowedResourcesResponse.from_json(ret)

    return ret

  def getSelfAllowedActionsBatch(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/authz/getSelfAllowedActionsBatch"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetSelfAllowedActionsBatchResponse import UacGetSelfAllowedActionsBatchResponse
      ret = UacGetSelfAllowedActionsBatchResponse.from_json(ret)

    return ret

  def getSelfAllowedResources(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/authz/getSelfAllowedResources"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetSelfAllowedResourcesResponse import UacGetSelfAllowedResourcesResponse
      ret = UacGetSelfAllowedResourcesResponse.from_json(ret)

    return ret

  def getSelfDirectlyAllowedResources(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/authz/getSelfDirectlyAllowedResources"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetSelfAllowedResourcesResponse import UacGetSelfAllowedResourcesResponse
      ret = UacGetSelfAllowedResourcesResponse.from_json(ret)

    return ret

  def isAllowed(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/authz/isAllowed"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacIsAllowedResponse import UacIsAllowedResponse
      ret = UacIsAllowedResponse.from_json(ret)

    return ret

  def isSelfAllowed(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/authz/isSelfAllowed"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacIsSelfAllowedResponse import UacIsSelfAllowedResponse
      ret = UacIsSelfAllowedResponse.from_json(ret)

    return ret
