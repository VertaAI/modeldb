# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class HydratedServiceApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def findHydratedDatasetVersions(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedDatasetVersions"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryDatasetVersionsResponse import ModeldbAdvancedQueryDatasetVersionsResponse
      ret = ModeldbAdvancedQueryDatasetVersionsResponse.from_json(ret)

    return ret

  def findHydratedDatasets(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedDatasets"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryDatasetsResponse import ModeldbAdvancedQueryDatasetsResponse
      ret = ModeldbAdvancedQueryDatasetsResponse.from_json(ret)

    return ret

  def findHydratedDatasetsByOrganization(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedDatasetsByOrganization"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryDatasetsResponse import ModeldbAdvancedQueryDatasetsResponse
      ret = ModeldbAdvancedQueryDatasetsResponse.from_json(ret)

    return ret

  def findHydratedDatasetsByTeam(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedDatasetsByTeam"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryDatasetsResponse import ModeldbAdvancedQueryDatasetsResponse
      ret = ModeldbAdvancedQueryDatasetsResponse.from_json(ret)

    return ret

  def findHydratedExperimentRuns(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedExperimentRuns"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryExperimentRunsResponse import ModeldbAdvancedQueryExperimentRunsResponse
      ret = ModeldbAdvancedQueryExperimentRunsResponse.from_json(ret)

    return ret

  def findHydratedExperiments(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedExperiments"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryExperimentsResponse import ModeldbAdvancedQueryExperimentsResponse
      ret = ModeldbAdvancedQueryExperimentsResponse.from_json(ret)

    return ret

  def findHydratedProjects(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedProjects"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryProjectsResponse import ModeldbAdvancedQueryProjectsResponse
      ret = ModeldbAdvancedQueryProjectsResponse.from_json(ret)

    return ret

  def findHydratedProjectsByOrganization(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedProjectsByOrganization"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryProjectsResponse import ModeldbAdvancedQueryProjectsResponse
      ret = ModeldbAdvancedQueryProjectsResponse.from_json(ret)

    return ret

  def findHydratedProjectsByTeam(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedProjectsByTeam"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryProjectsResponse import ModeldbAdvancedQueryProjectsResponse
      ret = ModeldbAdvancedQueryProjectsResponse.from_json(ret)

    return ret

  def findHydratedProjectsByUser(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedProjectsByUser"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryProjectsResponse import ModeldbAdvancedQueryProjectsResponse
      ret = ModeldbAdvancedQueryProjectsResponse.from_json(ret)

    return ret

  def findHydratedPublicDatasets(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedPublicDatasets"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryDatasetsResponse import ModeldbAdvancedQueryDatasetsResponse
      ret = ModeldbAdvancedQueryDatasetsResponse.from_json(ret)

    return ret

  def findHydratedPublicProjects(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/hydratedData/findHydratedPublicProjects"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbAdvancedQueryProjectsResponse import ModeldbAdvancedQueryProjectsResponse
      ret = ModeldbAdvancedQueryProjectsResponse.from_json(ret)

    return ret

  def getHydratedDatasetByName(self, name=None, workspace_name=None):
    __query = {
      "name": client.to_query(name),
      "workspace_name": client.to_query(workspace_name)
    }
    body = None

    format_args = {}
    path = "/hydratedData/getHydratedDatasetByName"
    if "$name" in path:
      path = path.replace("$name", "%(name)s")
      format_args["name"] = name
    if "$workspace_name" in path:
      path = path.replace("$workspace_name", "%(workspace_name)s")
      format_args["workspace_name"] = workspace_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetHydratedDatasetByNameResponse import ModeldbGetHydratedDatasetByNameResponse
      ret = ModeldbGetHydratedDatasetByNameResponse.from_json(ret)

    return ret

  def getHydratedDatasetsByProjectId(self, project_id=None, page_number=None, page_limit=None, ascending=None, sort_key=None):
    __query = {
      "project_id": client.to_query(project_id),
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key)
    }
    body = None

    format_args = {}
    path = "/hydratedData/getHydratedDatasetsByProjectId"
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
      from ..model.ModeldbGetHydratedDatasetsByProjectIdResponse import ModeldbGetHydratedDatasetsByProjectIdResponse
      ret = ModeldbGetHydratedDatasetsByProjectIdResponse.from_json(ret)

    return ret

  def getHydratedExperimentRunById(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/hydratedData/getHydratedExperimentRunById"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetHydratedExperimentRunByIdResponse import ModeldbGetHydratedExperimentRunByIdResponse
      ret = ModeldbGetHydratedExperimentRunByIdResponse.from_json(ret)

    return ret

  def getHydratedExperimentRunsInProject(self, project_id=None, page_number=None, page_limit=None, ascending=None, sort_key=None):
    __query = {
      "project_id": client.to_query(project_id),
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key)
    }
    body = None

    format_args = {}
    path = "/hydratedData/getHydratedExperimentRunsInProject"
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
      from ..model.ModeldbGetHydratedExperimentRunsByProjectIdResponse import ModeldbGetHydratedExperimentRunsByProjectIdResponse
      ret = ModeldbGetHydratedExperimentRunsByProjectIdResponse.from_json(ret)

    return ret

  def getHydratedExperimentsByProjectId(self, project_id=None, page_number=None, page_limit=None, ascending=None, sort_key=None):
    __query = {
      "project_id": client.to_query(project_id),
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key)
    }
    body = None

    format_args = {}
    path = "/hydratedData/getHydratedExperimentsByProjectId"
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
      from ..model.ModeldbGetHydratedExperimentsByProjectIdResponse import ModeldbGetHydratedExperimentsByProjectIdResponse
      ret = ModeldbGetHydratedExperimentsByProjectIdResponse.from_json(ret)

    return ret

  def getHydratedProjectById(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/hydratedData/getHydratedProjectById"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetHydratedProjectByIdResponse import ModeldbGetHydratedProjectByIdResponse
      ret = ModeldbGetHydratedProjectByIdResponse.from_json(ret)

    return ret

  def getHydratedProjects(self, page_number=None, page_limit=None, ascending=None, sort_key=None, workspace_name=None):
    __query = {
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key),
      "workspace_name": client.to_query(workspace_name)
    }
    body = None

    format_args = {}
    path = "/hydratedData/getHydratedProjects"
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
      from ..model.ModeldbGetHydratedProjectsResponse import ModeldbGetHydratedProjectsResponse
      ret = ModeldbGetHydratedProjectsResponse.from_json(ret)

    return ret

  def getHydratedPublicProjects(self, page_number=None, page_limit=None, ascending=None, sort_key=None, workspace_name=None):
    __query = {
      "page_number": client.to_query(page_number),
      "page_limit": client.to_query(page_limit),
      "ascending": client.to_query(ascending),
      "sort_key": client.to_query(sort_key),
      "workspace_name": client.to_query(workspace_name)
    }
    body = None

    format_args = {}
    path = "/hydratedData/getHydratedPublicProjects"
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
      from ..model.ModeldbGetHydratedProjectsResponse import ModeldbGetHydratedProjectsResponse
      ret = ModeldbGetHydratedProjectsResponse.from_json(ret)

    return ret

  def getTopHydratedExperimentRuns(self, project_id=None, experiment_id=None, experiment_run_ids=None, sort_key=None, ascending=None, top_k=None, ids_only=None):
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
    path = "/hydratedData/getTopHydratedExperimentRuns"
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
      from ..model.ModeldbAdvancedQueryExperimentRunsResponse import ModeldbAdvancedQueryExperimentRunsResponse
      ret = ModeldbAdvancedQueryExperimentRunsResponse.from_json(ret)

    return ret

  def sortHydratedExperimentRuns(self, experiment_run_ids=None, sort_key=None, ascending=None, ids_only=None):
    __query = {
      "experiment_run_ids": client.to_query(experiment_run_ids),
      "sort_key": client.to_query(sort_key),
      "ascending": client.to_query(ascending),
      "ids_only": client.to_query(ids_only)
    }
    body = None

    format_args = {}
    path = "/hydratedData/sortHydratedExperimentRuns"
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
      from ..model.ModeldbAdvancedQueryExperimentRunsResponse import ModeldbAdvancedQueryExperimentRunsResponse
      ret = ModeldbAdvancedQueryExperimentRunsResponse.from_json(ret)

    return ret
