# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class DatasetServiceApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def addDatasetAttributes(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/addDatasetAttributes"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddDatasetAttributesResponse import ModeldbAddDatasetAttributesResponse
      ret = ModeldbAddDatasetAttributesResponse.from_json(ret)

    return ret

  def addDatasetTags(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/addDatasetTags"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddDatasetTagsResponse import ModeldbAddDatasetTagsResponse
      ret = ModeldbAddDatasetTagsResponse.from_json(ret)

    return ret

  def createDataset(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/createDataset"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbCreateDatasetResponse import ModeldbCreateDatasetResponse
      ret = ModeldbCreateDatasetResponse.from_json(ret)

    return ret

  def deleteDataset(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/deleteDataset"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteDatasetResponse import ModeldbDeleteDatasetResponse
      ret = ModeldbDeleteDatasetResponse.from_json(ret)

    return ret

  def deleteDatasetAttributes(self, id=None, attribute_keys=None, delete_all=None):
    __query = {
      "id": client.to_query(id),
      "attribute_keys": client.to_query(attribute_keys),
      "delete_all": client.to_query(delete_all)
    }
    body = None

    format_args = {}
    path = "/dataset/deleteDatasetAttributes"
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
      from ..model.ModeldbDeleteDatasetAttributesResponse import ModeldbDeleteDatasetAttributesResponse
      ret = ModeldbDeleteDatasetAttributesResponse.from_json(ret)

    return ret

  def deleteDatasetTags(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/deleteDatasetTags"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteDatasetTagsResponse import ModeldbDeleteDatasetTagsResponse
      ret = ModeldbDeleteDatasetTagsResponse.from_json(ret)

    return ret

  def deleteDatasets(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/deleteDatasets"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteDatasetsResponse import ModeldbDeleteDatasetsResponse
      ret = ModeldbDeleteDatasetsResponse.from_json(ret)

    return ret

  def findDatasets(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/findDatasets"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbFindDatasetsResponse import ModeldbFindDatasetsResponse
      ret = ModeldbFindDatasetsResponse.from_json(ret)

    return ret

  def getAllDatasets(self, page_number=None, page_limit=None, ascending=None, sort_key=None, workspace_name=None):
    __query = {
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key),
      "workspace_name": client.to_query(workspace_name)
    }
    body = None

    format_args = {}
    path = "/dataset/getAllDatasets"
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
      from ..model.ModeldbGetAllDatasetsResponse import ModeldbGetAllDatasetsResponse
      ret = ModeldbGetAllDatasetsResponse.from_json(ret)

    return ret

  def getDatasetAttributes(self, id=None, attribute_keys=None, get_all=None):
    __query = {
      "id": client.to_query(id),
      "attribute_keys": client.to_query(attribute_keys),
      "get_all": client.to_query(get_all)
    }
    body = None

    format_args = {}
    path = "/dataset/getDatasetAttributes"
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

  def getDatasetById(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/dataset/getDatasetById"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetDatasetByIdResponse import ModeldbGetDatasetByIdResponse
      ret = ModeldbGetDatasetByIdResponse.from_json(ret)

    return ret

  def getDatasetByName(self, name=None, workspace_name=None):
    __query = {
      "name": client.to_query(name),
      "workspace_name": client.to_query(workspace_name)
    }
    body = None

    format_args = {}
    path = "/dataset/getDatasetByName"
    if "$name" in path:
      path = path.replace("$name", "%(name)s")
      format_args["name"] = name
    if "$workspace_name" in path:
      path = path.replace("$workspace_name", "%(workspace_name)s")
      format_args["workspace_name"] = workspace_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetDatasetByNameResponse import ModeldbGetDatasetByNameResponse
      ret = ModeldbGetDatasetByNameResponse.from_json(ret)

    return ret

  def getDatasetTags(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/dataset/getDatasetTags"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetTagsResponse import ModeldbGetTagsResponse
      ret = ModeldbGetTagsResponse.from_json(ret)

    return ret

  def getExperimentRunByDataset(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/getExperimentRunByDataset"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetExperimentRunByDatasetResponse import ModeldbGetExperimentRunByDatasetResponse
      ret = ModeldbGetExperimentRunByDatasetResponse.from_json(ret)

    return ret

  def getLastExperimentByDatasetId(self, dataset_id=None):
    __query = {
      "dataset_id": client.to_query(dataset_id)
    }
    body = None

    format_args = {}
    path = "/dataset/getLastExperimentByDatasetId"
    if "$dataset_id" in path:
      path = path.replace("$dataset_id", "%(dataset_id)s")
      format_args["dataset_id"] = dataset_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLastExperimentByDatasetIdResponse import ModeldbLastExperimentByDatasetIdResponse
      ret = ModeldbLastExperimentByDatasetIdResponse.from_json(ret)

    return ret

  def setDatasetVisibility(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/setDatasetVisibility"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbSetDatasetVisibiltyResponse import ModeldbSetDatasetVisibiltyResponse
      ret = ModeldbSetDatasetVisibiltyResponse.from_json(ret)

    return ret

  def setDatasetWorkspace(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/setDatasetWorkspace"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbSetDatasetWorkspaceResponse import ModeldbSetDatasetWorkspaceResponse
      ret = ModeldbSetDatasetWorkspaceResponse.from_json(ret)

    return ret

  def updateDatasetAttributes(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/updateDatasetAttributes"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateDatasetAttributesResponse import ModeldbUpdateDatasetAttributesResponse
      ret = ModeldbUpdateDatasetAttributesResponse.from_json(ret)

    return ret

  def updateDatasetDescription(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/updateDatasetDescription"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateDatasetDescriptionResponse import ModeldbUpdateDatasetDescriptionResponse
      ret = ModeldbUpdateDatasetDescriptionResponse.from_json(ret)

    return ret

  def updateDatasetName(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset/updateDatasetName"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateDatasetNameResponse import ModeldbUpdateDatasetNameResponse
      ret = ModeldbUpdateDatasetNameResponse.from_json(ret)

    return ret
