# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class RoleServiceApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def deleteRole(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/role/deleteRole"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacDeleteRoleResponse import UacDeleteRoleResponse
      ret = UacDeleteRoleResponse.from_json(ret)

    return ret

  def deleteRoleBinding(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/role/deleteRoleBinding"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacDeleteRoleBindingResponse import UacDeleteRoleBindingResponse
      ret = UacDeleteRoleBindingResponse.from_json(ret)

    return ret

  def getBindingRoleById(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/role/getRoleBindingById"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetRoleBindingByIdResponse import UacGetRoleBindingByIdResponse
      ret = UacGetRoleBindingByIdResponse.from_json(ret)

    return ret

  def getRoleBindingByName(self, name=None, scope_org_id=None, scope_team_id=None):
    __query = {
      "name": client.to_query(name),
      "scope.org_id": client.to_query(scope_org_id),
      "scope.team_id": client.to_query(scope_team_id)
    }
    body = None

    format_args = {}
    path = "/role/getRoleBindingByName"
    if "$name" in path:
      path = path.replace("$name", "%(name)s")
      format_args["name"] = name
    if "$scope_org_id" in path:
      path = path.replace("$scope_org_id", "%(scope_org_id)s")
      format_args["scope_org_id"] = scope_org_id
    if "$scope_team_id" in path:
      path = path.replace("$scope_team_id", "%(scope_team_id)s")
      format_args["scope_team_id"] = scope_team_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetRoleBindingByNameResponse import UacGetRoleBindingByNameResponse
      ret = UacGetRoleBindingByNameResponse.from_json(ret)

    return ret

  def getRoleById(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/role/getRoleById"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetRoleByIdResponse import UacGetRoleByIdResponse
      ret = UacGetRoleByIdResponse.from_json(ret)

    return ret

  def getRoleByName(self, name=None, scope_org_id=None, scope_team_id=None):
    __query = {
      "name": client.to_query(name),
      "scope.org_id": client.to_query(scope_org_id),
      "scope.team_id": client.to_query(scope_team_id)
    }
    body = None

    format_args = {}
    path = "/role/getRoleByName"
    if "$name" in path:
      path = path.replace("$name", "%(name)s")
      format_args["name"] = name
    if "$scope_org_id" in path:
      path = path.replace("$scope_org_id", "%(scope_org_id)s")
      format_args["scope_org_id"] = scope_org_id
    if "$scope_team_id" in path:
      path = path.replace("$scope_team_id", "%(scope_team_id)s")
      format_args["scope_team_id"] = scope_team_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetRoleByNameResponse import UacGetRoleByNameResponse
      ret = UacGetRoleByNameResponse.from_json(ret)

    return ret

  def listRoleBindings(self, entity_id=None, scope_org_id=None, scope_team_id=None):
    __query = {
      "entity_id": client.to_query(entity_id),
      "scope.org_id": client.to_query(scope_org_id),
      "scope.team_id": client.to_query(scope_team_id)
    }
    body = None

    format_args = {}
    path = "/role/listRoleBindings"
    if "$entity_id" in path:
      path = path.replace("$entity_id", "%(entity_id)s")
      format_args["entity_id"] = entity_id
    if "$scope_org_id" in path:
      path = path.replace("$scope_org_id", "%(scope_org_id)s")
      format_args["scope_org_id"] = scope_org_id
    if "$scope_team_id" in path:
      path = path.replace("$scope_team_id", "%(scope_team_id)s")
      format_args["scope_team_id"] = scope_team_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacListRoleBindingsResponse import UacListRoleBindingsResponse
      ret = UacListRoleBindingsResponse.from_json(ret)

    return ret

  def listRoles(self, scope_org_id=None, scope_team_id=None):
    __query = {
      "scope.org_id": client.to_query(scope_org_id),
      "scope.team_id": client.to_query(scope_team_id)
    }
    body = None

    format_args = {}
    path = "/role/listRoles"
    if "$scope_org_id" in path:
      path = path.replace("$scope_org_id", "%(scope_org_id)s")
      format_args["scope_org_id"] = scope_org_id
    if "$scope_team_id" in path:
      path = path.replace("$scope_team_id", "%(scope_team_id)s")
      format_args["scope_team_id"] = scope_team_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacListRolesResponse import UacListRolesResponse
      ret = UacListRolesResponse.from_json(ret)

    return ret

  def setRole(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/role/setRole"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacSetRoleResponse import UacSetRoleResponse
      ret = UacSetRoleResponse.from_json(ret)

    return ret

  def setRoleBinding(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/role/setRoleBinding"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacSetRoleBindingResponse import UacSetRoleBindingResponse
      ret = UacSetRoleBindingResponse.from_json(ret)

    return ret
