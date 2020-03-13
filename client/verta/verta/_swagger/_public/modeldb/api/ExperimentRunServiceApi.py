# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class ExperimentRunServiceApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def addExperimentRunAttributes(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/addExperimentRunAttributes"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddExperimentRunAttributesResponse import ModeldbAddExperimentRunAttributesResponse
      ret = ModeldbAddExperimentRunAttributesResponse.from_json(ret)

    return ret

  def addExperimentRunTag(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/addExperimentRunTag"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddExperimentRunTagResponse import ModeldbAddExperimentRunTagResponse
      ret = ModeldbAddExperimentRunTagResponse.from_json(ret)

    return ret

  def addExperimentRunTags(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/addExperimentRunTags"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAddExperimentRunTagsResponse import ModeldbAddExperimentRunTagsResponse
      ret = ModeldbAddExperimentRunTagsResponse.from_json(ret)

    return ret

  def createExperimentRun(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/createExperimentRun"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbCreateExperimentRunResponse import ModeldbCreateExperimentRunResponse
      ret = ModeldbCreateExperimentRunResponse.from_json(ret)

    return ret

  def deleteArtifact(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/deleteArtifact"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteArtifactResponse import ModeldbDeleteArtifactResponse
      ret = ModeldbDeleteArtifactResponse.from_json(ret)

    return ret

  def deleteExperimentRun(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/deleteExperimentRun"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteExperimentRunResponse import ModeldbDeleteExperimentRunResponse
      ret = ModeldbDeleteExperimentRunResponse.from_json(ret)

    return ret

  def deleteExperimentRunAttributes(self, id=None, attribute_keys=None, delete_all=None):
    __query = {
      "id": client.to_query(id),
      "attribute_keys": client.to_query(attribute_keys),
      "delete_all": client.to_query(delete_all)
    }
    body = None

    format_args = {}
    path = "/experiment-run/deleteExperimentRunAttributes"
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
      from ..model.ModeldbDeleteExperimentRunAttributesResponse import ModeldbDeleteExperimentRunAttributesResponse
      ret = ModeldbDeleteExperimentRunAttributesResponse.from_json(ret)

    return ret

  def deleteExperimentRunTag(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/deleteExperimentRunTag"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteExperimentRunTagResponse import ModeldbDeleteExperimentRunTagResponse
      ret = ModeldbDeleteExperimentRunTagResponse.from_json(ret)

    return ret

  def deleteExperimentRunTags(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/deleteExperimentRunTags"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteExperimentRunTagsResponse import ModeldbDeleteExperimentRunTagsResponse
      ret = ModeldbDeleteExperimentRunTagsResponse.from_json(ret)

    return ret

  def deleteExperimentRuns(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/deleteExperimentRuns"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("DELETE", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteExperimentRunsResponse import ModeldbDeleteExperimentRunsResponse
      ret = ModeldbDeleteExperimentRunsResponse.from_json(ret)

    return ret

  def findExperimentRuns(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/findExperimentRuns"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbFindExperimentRunsResponse import ModeldbFindExperimentRunsResponse
      ret = ModeldbFindExperimentRunsResponse.from_json(ret)

    return ret

  def getArtifacts(self, id=None, key=None):
    __query = {
      "id": client.to_query(id),
      "key": client.to_query(key)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getArtifacts"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    if "$key" in path:
      path = path.replace("$key", "%(key)s")
      format_args["key"] = key
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetArtifactsResponse import ModeldbGetArtifactsResponse
      ret = ModeldbGetArtifactsResponse.from_json(ret)

    return ret

  def getChildrenExperimentRuns(self, experiment_run_id=None, page_number=None, page_limit=None, ascending=None, sort_key=None):
    __query = {
      "experiment_run_id": client.to_query(experiment_run_id),
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getChildrenExperimentRuns"
    if "$experiment_run_id" in path:
      path = path.replace("$experiment_run_id", "%(experiment_run_id)s")
      format_args["experiment_run_id"] = experiment_run_id
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
      from ..model.ModeldbGetChildrenExperimentRunsResponse import ModeldbGetChildrenExperimentRunsResponse
      ret = ModeldbGetChildrenExperimentRunsResponse.from_json(ret)

    return ret

  def getDatasets(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getDatasets"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetDatasetsResponse import ModeldbGetDatasetsResponse
      ret = ModeldbGetDatasetsResponse.from_json(ret)

    return ret

  def getExperimentRunAttributes(self, id=None, attribute_keys=None, get_all=None):
    __query = {
      "id": client.to_query(id),
      "attribute_keys": client.to_query(attribute_keys),
      "get_all": client.to_query(get_all)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getAttributes"
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

  def getExperimentRunById(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getExperimentRunById"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetExperimentRunByIdResponse import ModeldbGetExperimentRunByIdResponse
      ret = ModeldbGetExperimentRunByIdResponse.from_json(ret)

    return ret

  def getExperimentRunByName(self, name=None, experiment_id=None):
    __query = {
      "name": client.to_query(name),
      "experiment_id": client.to_query(experiment_id)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getExperimentRunByName"
    if "$name" in path:
      path = path.replace("$name", "%(name)s")
      format_args["name"] = name
    if "$experiment_id" in path:
      path = path.replace("$experiment_id", "%(experiment_id)s")
      format_args["experiment_id"] = experiment_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetExperimentRunByNameResponse import ModeldbGetExperimentRunByNameResponse
      ret = ModeldbGetExperimentRunByNameResponse.from_json(ret)

    return ret

  def getExperimentRunCodeVersion(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getExperimentRunCodeVersion"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetExperimentRunCodeVersionResponse import ModeldbGetExperimentRunCodeVersionResponse
      ret = ModeldbGetExperimentRunCodeVersionResponse.from_json(ret)

    return ret

  def getExperimentRunTags(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getExperimentRunTags"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetTagsResponse import ModeldbGetTagsResponse
      ret = ModeldbGetTagsResponse.from_json(ret)

    return ret

  def getExperimentRunsByDatasetVersionId(self, datset_version_id=None, page_number=None, page_limit=None, ascending=None, sort_key=None):
    __query = {
      "datset_version_id": client.to_query(datset_version_id),
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getExperimentRunsByDatasetVersionId"
    if "$datset_version_id" in path:
      path = path.replace("$datset_version_id", "%(datset_version_id)s")
      format_args["datset_version_id"] = datset_version_id
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
      from ..model.ModeldbGetExperimentRunsByDatasetVersionIdResponse import ModeldbGetExperimentRunsByDatasetVersionIdResponse
      ret = ModeldbGetExperimentRunsByDatasetVersionIdResponse.from_json(ret)

    return ret

  def getExperimentRunsInExperiment(self, experiment_id=None, page_number=None, page_limit=None, ascending=None, sort_key=None):
    __query = {
      "experiment_id": client.to_query(experiment_id),
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getExperimentRunsInExperiment"
    if "$experiment_id" in path:
      path = path.replace("$experiment_id", "%(experiment_id)s")
      format_args["experiment_id"] = experiment_id
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
      from ..model.ModeldbGetExperimentRunsInExperimentResponse import ModeldbGetExperimentRunsInExperimentResponse
      ret = ModeldbGetExperimentRunsInExperimentResponse.from_json(ret)

    return ret

  def getExperimentRunsInProject(self, project_id=None, page_number=None, page_limit=None, ascending=None, sort_key=None):
    __query = {
      "project_id": client.to_query(project_id),
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getExperimentRunsInProject"
    if "$project_id" in path:
      path = path.replace("$project_id", "%(project_id)s")
      format_args["project_id"] = project_id
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
      from ..model.ModeldbGetExperimentRunsInProjectResponse import ModeldbGetExperimentRunsInProjectResponse
      ret = ModeldbGetExperimentRunsInProjectResponse.from_json(ret)

    return ret

  def getHyperparameters(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getHyperparameters"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetHyperparametersResponse import ModeldbGetHyperparametersResponse
      ret = ModeldbGetHyperparametersResponse.from_json(ret)

    return ret

  def getJobId(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getJobId"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetJobIdResponse import ModeldbGetJobIdResponse
      ret = ModeldbGetJobIdResponse.from_json(ret)

    return ret

  def getMetrics(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getMetrics"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetMetricsResponse import ModeldbGetMetricsResponse
      ret = ModeldbGetMetricsResponse.from_json(ret)

    return ret

  def getObservations(self, id=None, observation_key=None):
    __query = {
      "id": client.to_query(id),
      "observation_key": client.to_query(observation_key)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getObservations"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    if "$observation_key" in path:
      path = path.replace("$observation_key", "%(observation_key)s")
      format_args["observation_key"] = observation_key
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetObservationsResponse import ModeldbGetObservationsResponse
      ret = ModeldbGetObservationsResponse.from_json(ret)

    return ret

  def getTopExperimentRuns(self, project_id=None, experiment_id=None, experiment_run_ids=None, sort_key=None, ascending=None, top_k=None, ids_only=None):
    __query = {
      "project_id": client.to_query(project_id),
      "experiment_id": client.to_query(experiment_id),
      "experiment_run_ids": client.to_query(experiment_run_ids),
      "sort_key": client.to_query(sort_key),
      "ascending": client.to_query(ascending),
      "top_k": client.to_query(top_k),
      "ids_only": client.to_query(ids_only)
    }
    body = None

    format_args = {}
    path = "/experiment-run/getTopExperimentRuns"
    if "$project_id" in path:
      path = path.replace("$project_id", "%(project_id)s")
      format_args["project_id"] = project_id
    if "$experiment_id" in path:
      path = path.replace("$experiment_id", "%(experiment_id)s")
      format_args["experiment_id"] = experiment_id
    if "$experiment_run_ids" in path:
      path = path.replace("$experiment_run_ids", "%(experiment_run_ids)s")
      format_args["experiment_run_ids"] = experiment_run_ids
    if "$sort_key" in path:
      path = path.replace("$sort_key", "%(sort_key)s")
      format_args["sort_key"] = sort_key
    if "$ascending" in path:
      path = path.replace("$ascending", "%(ascending)s")
      format_args["ascending"] = ascending
    if "$top_k" in path:
      path = path.replace("$top_k", "%(top_k)s")
      format_args["top_k"] = top_k
    if "$ids_only" in path:
      path = path.replace("$ids_only", "%(ids_only)s")
      format_args["ids_only"] = ids_only
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbTopExperimentRunsSelectorResponse import ModeldbTopExperimentRunsSelectorResponse
      ret = ModeldbTopExperimentRunsSelectorResponse.from_json(ret)

    return ret

  def getUrlForArtifact(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/getUrlForArtifact"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetUrlForArtifactResponse import ModeldbGetUrlForArtifactResponse
      ret = ModeldbGetUrlForArtifactResponse.from_json(ret)

    return ret

  def logArtifact(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logArtifact"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogArtifactResponse import ModeldbLogArtifactResponse
      ret = ModeldbLogArtifactResponse.from_json(ret)

    return ret

  def logArtifacts(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logArtifacts"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogArtifactsResponse import ModeldbLogArtifactsResponse
      ret = ModeldbLogArtifactsResponse.from_json(ret)

    return ret

  def logAttribute(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logAttribute"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogAttributeResponse import ModeldbLogAttributeResponse
      ret = ModeldbLogAttributeResponse.from_json(ret)

    return ret

  def logAttributes(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logAttributes"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogAttributesResponse import ModeldbLogAttributesResponse
      ret = ModeldbLogAttributesResponse.from_json(ret)

    return ret

  def logDataset(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logDataset"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogDatasetResponse import ModeldbLogDatasetResponse
      ret = ModeldbLogDatasetResponse.from_json(ret)

    return ret

  def logDatasets(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logDatasets"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogDatasetsResponse import ModeldbLogDatasetsResponse
      ret = ModeldbLogDatasetsResponse.from_json(ret)

    return ret

  def logExperimentRunCodeVersion(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logExperimentRunCodeVersion"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogExperimentRunCodeVersionResponse import ModeldbLogExperimentRunCodeVersionResponse
      ret = ModeldbLogExperimentRunCodeVersionResponse.from_json(ret)

    return ret

  def logHyperparameter(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logHyperparameter"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogHyperparameterResponse import ModeldbLogHyperparameterResponse
      ret = ModeldbLogHyperparameterResponse.from_json(ret)

    return ret

  def logHyperparameters(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logHyperparameters"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogHyperparametersResponse import ModeldbLogHyperparametersResponse
      ret = ModeldbLogHyperparametersResponse.from_json(ret)

    return ret

  def logJobId(self, id=None, job_id=None):
    __query = {
      "id": client.to_query(id),
      "job_id": client.to_query(job_id)
    }
    body = None

    format_args = {}
    path = "/experiment-run/logJobId"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    if "$job_id" in path:
      path = path.replace("$job_id", "%(job_id)s")
      format_args["job_id"] = job_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogJobIdResponse import ModeldbLogJobIdResponse
      ret = ModeldbLogJobIdResponse.from_json(ret)

    return ret

  def logMetric(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logMetric"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogMetricResponse import ModeldbLogMetricResponse
      ret = ModeldbLogMetricResponse.from_json(ret)

    return ret

  def logMetrics(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logMetrics"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogMetricsResponse import ModeldbLogMetricsResponse
      ret = ModeldbLogMetricsResponse.from_json(ret)

    return ret

  def logObservation(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logObservation"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogObservationResponse import ModeldbLogObservationResponse
      ret = ModeldbLogObservationResponse.from_json(ret)

    return ret

  def logObservations(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/logObservations"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbLogObservationsResponse import ModeldbLogObservationsResponse
      ret = ModeldbLogObservationsResponse.from_json(ret)

    return ret

  def setParentExperimentRunId(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/setParentExperimentRunId"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbSetParentExperimentRunIdResponse import ModeldbSetParentExperimentRunIdResponse
      ret = ModeldbSetParentExperimentRunIdResponse.from_json(ret)

    return ret

  def sortExperimentRuns(self, experiment_run_ids=None, sort_key=None, ascending=None, ids_only=None):
    __query = {
      "experiment_run_ids": client.to_query(experiment_run_ids),
      "sort_key": client.to_query(sort_key),
      "ascending": client.to_query(ascending),
      "ids_only": client.to_query(ids_only)
    }
    body = None

    format_args = {}
    path = "/experiment-run/sortExperimentRuns"
    if "$experiment_run_ids" in path:
      path = path.replace("$experiment_run_ids", "%(experiment_run_ids)s")
      format_args["experiment_run_ids"] = experiment_run_ids
    if "$sort_key" in path:
      path = path.replace("$sort_key", "%(sort_key)s")
      format_args["sort_key"] = sort_key
    if "$ascending" in path:
      path = path.replace("$ascending", "%(ascending)s")
      format_args["ascending"] = ascending
    if "$ids_only" in path:
      path = path.replace("$ids_only", "%(ids_only)s")
      format_args["ids_only"] = ids_only
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbSortExperimentRunsResponse import ModeldbSortExperimentRunsResponse
      ret = ModeldbSortExperimentRunsResponse.from_json(ret)

    return ret

  def updateExperimentRunDescription(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/updateExperimentRunDescription"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateExperimentRunDescriptionResponse import ModeldbUpdateExperimentRunDescriptionResponse
      ret = ModeldbUpdateExperimentRunDescriptionResponse.from_json(ret)

    return ret

  def updateExperimentRunName(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/experiment-run/updateExperimentRunName"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateExperimentRunNameResponse import ModeldbUpdateExperimentRunNameResponse
      ret = ModeldbUpdateExperimentRunNameResponse.from_json(ret)

    return ret
