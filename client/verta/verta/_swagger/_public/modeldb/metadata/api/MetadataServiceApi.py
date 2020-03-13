# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class MetadataServiceApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def AddLabels(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/metadata/labels"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("PUT", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.MetadataAddLabelsRequestResponse import MetadataAddLabelsRequestResponse
      ret = MetadataAddLabelsRequestResponse.from_json(ret)

    return ret

  def DeleteLabels(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/metadata/delete"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.MetadataDeleteLabelsRequestResponse import MetadataDeleteLabelsRequestResponse
      ret = MetadataDeleteLabelsRequestResponse.from_json(ret)

    return ret

  def GetLabels(self, id_id_type=None, id_int_id=None, id_string_id=None):
    __query = {
      "id.id_type": client.to_query(id_id_type),
      "id.int_id": client.to_query(id_int_id),
      "id.string_id": client.to_query(id_string_id)
    }
    body = None

    format_args = {}
    path = "/metadata/labels"
    if "$id_id_type" in path:
      path = path.replace("$id_id_type", "%(id_id_type)s")
      format_args["id_id_type"] = id_id_type
    if "$id_int_id" in path:
      path = path.replace("$id_int_id", "%(id_int_id)s")
      format_args["id_int_id"] = id_int_id
    if "$id_string_id" in path:
      path = path.replace("$id_string_id", "%(id_string_id)s")
      format_args["id_string_id"] = id_string_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.MetadataGetLabelsRequestResponse import MetadataGetLabelsRequestResponse
      ret = MetadataGetLabelsRequestResponse.from_json(ret)

    return ret
