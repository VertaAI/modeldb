# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class CollaboratorApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def addOrUpdateDatasetCollaborator(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/collaborator/addOrUpdateDatasetCollaborator"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacAddCollaboratorRequestResponse import UacAddCollaboratorRequestResponse
      ret = UacAddCollaboratorRequestResponse.from_json(ret)

    return ret

  def addOrUpdateProjectCollaborator(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/collaborator/addOrUpdateProjectCollaborator"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacAddCollaboratorRequestResponse import UacAddCollaboratorRequestResponse
      ret = UacAddCollaboratorRequestResponse.from_json(ret)

    return ret

  def getDatasetCollaborators(self, entity_id=None):
    __query = {
      "entity_id": client.to_query(entity_id)
    }
    body = None

    format_args = {}
    path = "/collaborator/getDatasetCollaborators"
    if "$entity_id" in path:
      path = path.replace("$entity_id", "%(entity_id)s")
      format_args["entity_id"] = entity_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetCollaboratorResponse import UacGetCollaboratorResponse
      ret = UacGetCollaboratorResponse.from_json(ret)

    return ret

  def getProjectCollaborators(self, entity_id=None):
    __query = {
      "entity_id": client.to_query(entity_id)
    }
    body = None

    format_args = {}
    path = "/collaborator/getProjectCollaborators"
    if "$entity_id" in path:
      path = path.replace("$entity_id", "%(entity_id)s")
      format_args["entity_id"] = entity_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetCollaboratorResponse import UacGetCollaboratorResponse
      ret = UacGetCollaboratorResponse.from_json(ret)

    return ret

  def removeDatasetCollaborator(self, entity_id=None, share_with=None, date_deleted=None, authz_entity_type=None):
    __query = {
      "entity_id": client.to_query(entity_id),
      "share_with": client.to_query(share_with),
      "date_deleted": client.to_query(date_deleted),
      "authz_entity_type": client.to_query(authz_entity_type)
    }
    body = None

    format_args = {}
    path = "/collaborator/removeDatasetCollaborator"
    if "$entity_id" in path:
      path = path.replace("$entity_id", "%(entity_id)s")
      format_args["entity_id"] = entity_id
    if "$share_with" in path:
      path = path.replace("$share_with", "%(share_with)s")
      format_args["share_with"] = share_with
    if "$date_deleted" in path:
      path = path.replace("$date_deleted", "%(date_deleted)s")
      format_args["date_deleted"] = date_deleted
    if "$authz_entity_type" in path:
      path = path.replace("$authz_entity_type", "%(authz_entity_type)s")
      format_args["authz_entity_type"] = authz_entity_type
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacRemoveCollaboratorResponse import UacRemoveCollaboratorResponse
      ret = UacRemoveCollaboratorResponse.from_json(ret)

    return ret

  def removeProjectCollaborator(self, entity_id=None, share_with=None, date_deleted=None, authz_entity_type=None):
    __query = {
      "entity_id": client.to_query(entity_id),
      "share_with": client.to_query(share_with),
      "date_deleted": client.to_query(date_deleted),
      "authz_entity_type": client.to_query(authz_entity_type)
    }
    body = None

    format_args = {}
    path = "/collaborator/removeProjectCollaborator"
    if "$entity_id" in path:
      path = path.replace("$entity_id", "%(entity_id)s")
      format_args["entity_id"] = entity_id
    if "$share_with" in path:
      path = path.replace("$share_with", "%(share_with)s")
      format_args["share_with"] = share_with
    if "$date_deleted" in path:
      path = path.replace("$date_deleted", "%(date_deleted)s")
      format_args["date_deleted"] = date_deleted
    if "$authz_entity_type" in path:
      path = path.replace("$authz_entity_type", "%(authz_entity_type)s")
      format_args["authz_entity_type"] = authz_entity_type
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacRemoveCollaboratorResponse import UacRemoveCollaboratorResponse
      ret = UacRemoveCollaboratorResponse.from_json(ret)

    return ret
