# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class CommentApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def addExperimentRunComment(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/comment/addExperimentRunComment"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddCommentResponse import ModeldbAddCommentResponse
      ret = ModeldbAddCommentResponse.from_json(ret)

    return ret

  def deleteExperimentRunComment(self, id=None, entity_id=None):
    __query = {
      "id": client.to_query(id),
      "entity_id": client.to_query(entity_id)
    }
    body = None

    format_args = {}
    path = "/comment/deleteExperimentRunComment"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    if "$entity_id" in path:
      path = path.replace("$entity_id", "%(entity_id)s")
      format_args["entity_id"] = entity_id
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteCommentResponse import ModeldbDeleteCommentResponse
      ret = ModeldbDeleteCommentResponse.from_json(ret)

    return ret

  def getExperimentRunComments(self, entity_id=None):
    __query = {
      "entity_id": client.to_query(entity_id)
    }
    body = None

    format_args = {}
    path = "/comment/getExperimentRunComments"
    if "$entity_id" in path:
      path = path.replace("$entity_id", "%(entity_id)s")
      format_args["entity_id"] = entity_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetCommentsResponse import ModeldbGetCommentsResponse
      ret = ModeldbGetCommentsResponse.from_json(ret)

    return ret

  def updateExperimentRunComment(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/comment/updateExperimentRunComment"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateCommentResponse import ModeldbUpdateCommentResponse
      ret = ModeldbUpdateCommentResponse.from_json(ret)

    return ret
