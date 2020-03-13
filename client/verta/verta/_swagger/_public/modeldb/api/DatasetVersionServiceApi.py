# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class DatasetVersionServiceApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def addDatasetVersionAttributes(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset-version/addDatasetVersionAttributes"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddDatasetVersionAttributesResponse import ModeldbAddDatasetVersionAttributesResponse
      ret = ModeldbAddDatasetVersionAttributesResponse.from_json(ret)

    return ret

  def addDatasetVersionTags(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset-version/addDatasetVersionTags"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddDatasetVersionTagsResponse import ModeldbAddDatasetVersionTagsResponse
      ret = ModeldbAddDatasetVersionTagsResponse.from_json(ret)

    return ret

  def createDatasetVersion(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset-version/createDatasetVersion"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbCreateDatasetVersionResponse import ModeldbCreateDatasetVersionResponse
      ret = ModeldbCreateDatasetVersionResponse.from_json(ret)

    return ret

  def deleteDatasetVersion(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset-version/deleteDatasetVersion"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteDatasetVersionResponse import ModeldbDeleteDatasetVersionResponse
      ret = ModeldbDeleteDatasetVersionResponse.from_json(ret)

    return ret

  def deleteDatasetVersionAttributes(self, id=None, attribute_keys=None, delete_all=None):
    __query = {
      "id": client.to_query(id),
      "attribute_keys": client.to_query(attribute_keys),
      "delete_all": client.to_query(delete_all)
    }
    body = None

    format_args = {}
    path = "/dataset-version/deleteDatasetVersionAttributes"
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
      from ..model.ModeldbDeleteDatasetVersionAttributesResponse import ModeldbDeleteDatasetVersionAttributesResponse
      ret = ModeldbDeleteDatasetVersionAttributesResponse.from_json(ret)

    return ret

  def deleteDatasetVersionTags(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset-version/deleteDatasetVersionTags"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteDatasetVersionTagsResponse import ModeldbDeleteDatasetVersionTagsResponse
      ret = ModeldbDeleteDatasetVersionTagsResponse.from_json(ret)

    return ret

  def deleteDatasetVersions(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset-version/deleteDatasetVersions"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteDatasetVersionsResponse import ModeldbDeleteDatasetVersionsResponse
      ret = ModeldbDeleteDatasetVersionsResponse.from_json(ret)

    return ret

  def findDatasetVersions(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset-version/findDatasetVersions"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbFindDatasetVersionsResponse import ModeldbFindDatasetVersionsResponse
      ret = ModeldbFindDatasetVersionsResponse.from_json(ret)

    return ret

  def getAllDatasetVersionsByDatasetId(self, dataset_id=None, page_number=None, page_limit=None, ascending=None, sort_key=None):
    __query = {
      "dataset_id": client.to_query(dataset_id),
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key)
    }
    body = None

    format_args = {}
    path = "/dataset-version/getAllDatasetVersionsByDatasetId"
    if "$dataset_id" in path:
      path = path.replace("$dataset_id", "%(dataset_id)s")
      format_args["dataset_id"] = dataset_id
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
      from ..model.ModeldbGetAllDatasetVersionsByDatasetIdResponse import ModeldbGetAllDatasetVersionsByDatasetIdResponse
      ret = ModeldbGetAllDatasetVersionsByDatasetIdResponse.from_json(ret)

    return ret

  def getDatasetVersionAttributes(self, id=None, attribute_keys=None, get_all=None):
    __query = {
      "id": client.to_query(id),
      "attribute_keys": client.to_query(attribute_keys),
      "get_all": client.to_query(get_all)
    }
    body = None

    format_args = {}
    path = "/dataset-version/getDatasetVersionAttributes"
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

  def getDatasetVersionById(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/dataset-version/getDatasetVersionById"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetDatasetVersionByIdResponse import ModeldbGetDatasetVersionByIdResponse
      ret = ModeldbGetDatasetVersionByIdResponse.from_json(ret)

    return ret

  def getDatasetVersionTags(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/dataset-version/getDatasetVersionTags"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetTagsResponse import ModeldbGetTagsResponse
      ret = ModeldbGetTagsResponse.from_json(ret)

    return ret

  def getLatestDatasetVersionByDatasetId(self, dataset_id=None, ascending=None, sort_key=None):
    __query = {
      "dataset_id": client.to_query(dataset_id),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key)
    }
    body = None

    format_args = {}
    path = "/dataset-version/getLatestDatasetVersionByDatasetId"
    if "$dataset_id" in path:
      path = path.replace("$dataset_id", "%(dataset_id)s")
      format_args["dataset_id"] = dataset_id
    if "$ascending" in path:
      path = path.replace("$ascending", "%(ascending)s")
      format_args["ascending"] = ascending
    if "$sort_key" in path:
      path = path.replace("$sort_key", "%(sort_key)s")
      format_args["sort_key"] = sort_key
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetLatestDatasetVersionByDatasetIdResponse import ModeldbGetLatestDatasetVersionByDatasetIdResponse
      ret = ModeldbGetLatestDatasetVersionByDatasetIdResponse.from_json(ret)

    return ret

  def setDatasetVersionVisibility(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset-version/setDatasetVersionVisibility"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbSetDatasetVersionVisibiltyResponse import ModeldbSetDatasetVersionVisibiltyResponse
      ret = ModeldbSetDatasetVersionVisibiltyResponse.from_json(ret)

    return ret

  def updateDatasetVersionAttributes(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset-version/updateDatasetVersionAttributes"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateDatasetVersionAttributesResponse import ModeldbUpdateDatasetVersionAttributesResponse
      ret = ModeldbUpdateDatasetVersionAttributesResponse.from_json(ret)

    return ret

  def updateDatasetVersionDescription(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/dataset-version/updateDatasetVersionDescription"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateDatasetVersionDescriptionResponse import ModeldbUpdateDatasetVersionDescriptionResponse
      ret = ModeldbUpdateDatasetVersionDescriptionResponse.from_json(ret)

    return ret
