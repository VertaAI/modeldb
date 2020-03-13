# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class ArtifactStoreApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def deleteArtifact(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/artifact/deleteArtifact"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ArtifactstoreDeleteArtifactResponse import ArtifactstoreDeleteArtifactResponse
      ret = ArtifactstoreDeleteArtifactResponse.from_json(ret)

    return ret

  def getArtifact(self, key=None):
    __query = {
      "key": client.to_query(key)
    }
    body = None

    format_args = {}
    path = "/artifact/getArtifact"
    if "$key" in path:
      path = path.replace("$key", "%(key)s")
      format_args["key"] = key
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ArtifactstoreGetArtifactResponse import ArtifactstoreGetArtifactResponse
      ret = ArtifactstoreGetArtifactResponse.from_json(ret)

    return ret

  def storeArtifact(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/artifact/storeArtifact"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ArtifactstoreStoreArtifactResponse import ArtifactstoreStoreArtifactResponse
      ret = ArtifactstoreStoreArtifactResponse.from_json(ret)

    return ret

  def storeArtifactWithStream(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/artifact/storeArtifactWithStream"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ArtifactstoreStoreArtifactWithStreamResponse import ArtifactstoreStoreArtifactWithStreamResponse
      ret = ArtifactstoreStoreArtifactWithStreamResponse.from_json(ret)

    return ret
