# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class OrganizationApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def addUser(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/organization/addUser"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacAddUserResponse import UacAddUserResponse
      ret = UacAddUserResponse.from_json(ret)

    return ret

  def deleteOrganization(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/organization/deleteOrganization"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacDeleteOrganizationResponse import UacDeleteOrganizationResponse
      ret = UacDeleteOrganizationResponse.from_json(ret)

    return ret

  def getOrganizationById(self, org_id=None):
    __query = {
      "org_id": client.to_query(org_id)
    }
    body = None

    format_args = {}
    path = "/organization/getOrganizationById"
    if "$org_id" in path:
      path = path.replace("$org_id", "%(org_id)s")
      format_args["org_id"] = org_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetOrganizationByIdResponse import UacGetOrganizationByIdResponse
      ret = UacGetOrganizationByIdResponse.from_json(ret)

    return ret

  def getOrganizationByName(self, org_name=None):
    __query = {
      "org_name": client.to_query(org_name)
    }
    body = None

    format_args = {}
    path = "/organization/getOrganizationByName"
    if "$org_name" in path:
      path = path.replace("$org_name", "%(org_name)s")
      format_args["org_name"] = org_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetOrganizationByNameResponse import UacGetOrganizationByNameResponse
      ret = UacGetOrganizationByNameResponse.from_json(ret)

    return ret

  def getOrganizationByShortName(self, short_name=None):
    __query = {
      "short_name": client.to_query(short_name)
    }
    body = None

    format_args = {}
    path = "/organization/getOrganizationByShortName"
    if "$short_name" in path:
      path = path.replace("$short_name", "%(short_name)s")
      format_args["short_name"] = short_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetOrganizationByShortNameResponse import UacGetOrganizationByShortNameResponse
      ret = UacGetOrganizationByShortNameResponse.from_json(ret)

    return ret

  def listMyOrganizations(self, ):
    __query = {
    }
    body = None

    format_args = {}
    path = "/organization/listMyOrganizations"
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacListMyOrganizationsResponse import UacListMyOrganizationsResponse
      ret = UacListMyOrganizationsResponse.from_json(ret)

    return ret

  def listTeams(self, org_id=None):
    __query = {
      "org_id": client.to_query(org_id)
    }
    body = None

    format_args = {}
    path = "/organization/listTeams"
    if "$org_id" in path:
      path = path.replace("$org_id", "%(org_id)s")
      format_args["org_id"] = org_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacListTeamsResponse import UacListTeamsResponse
      ret = UacListTeamsResponse.from_json(ret)

    return ret

  def listUsers(self, org_id=None):
    __query = {
      "org_id": client.to_query(org_id)
    }
    body = None

    format_args = {}
    path = "/organization/listUsers"
    if "$org_id" in path:
      path = path.replace("$org_id", "%(org_id)s")
      format_args["org_id"] = org_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacListUsersResponse import UacListUsersResponse
      ret = UacListUsersResponse.from_json(ret)

    return ret

  def removeUser(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/organization/removeUser"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacRemoveUserResponse import UacRemoveUserResponse
      ret = UacRemoveUserResponse.from_json(ret)

    return ret

  def setOrganization(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/organization/setOrganization"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacSetOrganizationResponse import UacSetOrganizationResponse
      ret = UacSetOrganizationResponse.from_json(ret)

    return ret
