# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class LineageApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def addLineage(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/lineage/addLineage"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddLineageResponse import ModeldbAddLineageResponse
      ret = ModeldbAddLineageResponse.from_json(ret)

    return ret

  def deleteLineage(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/lineage/deleteLineage"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteLineageResponse import ModeldbDeleteLineageResponse
      ret = ModeldbDeleteLineageResponse.from_json(ret)

    return ret

  def findAllInputs(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/lineage/findAllInputs"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbFindAllInputsResponse import ModeldbFindAllInputsResponse
      ret = ModeldbFindAllInputsResponse.from_json(ret)

    return ret

  def findAllInputsOutputs(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/lineage/findAllInputsOutputs"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbFindAllInputsOutputsResponse import ModeldbFindAllInputsOutputsResponse
      ret = ModeldbFindAllInputsOutputsResponse.from_json(ret)

    return ret

  def findAllOutputs(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/lineage/findAllOutputs"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbFindAllOutputsResponse import ModeldbFindAllOutputsResponse
      ret = ModeldbFindAllOutputsResponse.from_json(ret)

    return ret
