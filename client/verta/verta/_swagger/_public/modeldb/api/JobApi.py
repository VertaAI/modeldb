# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class JobApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def createJob(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/job/createJob"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbCreateJobResponse import ModeldbCreateJobResponse
      ret = ModeldbCreateJobResponse.from_json(ret)

    return ret

  def deleteJob(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/job/deleteJob"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbDeleteJobResponse import ModeldbDeleteJobResponse
      ret = ModeldbDeleteJobResponse.from_json(ret)

    return ret

  def getJob(self, id=None):
    __query = {
      "id": client.to_query(id)
    }
    body = None

    format_args = {}
    path = "/job/getJob"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbGetJobResponse import ModeldbGetJobResponse
      ret = ModeldbGetJobResponse.from_json(ret)

    return ret

  def updateJob(self, id=None, end_time=None, job_status=None):
    __query = {
      "id": client.to_query(id),
      "end_time": client.to_query(end_time),
      "job_status": client.to_query(job_status)
    }
    body = None

    format_args = {}
    path = "/job/updateJob"
    if "$id" in path:
      path = path.replace("$id", "%(id)s")
      format_args["id"] = id
    if "$end_time" in path:
      path = path.replace("$end_time", "%(end_time)s")
      format_args["end_time"] = end_time
    if "$job_status" in path:
      path = path.replace("$job_status", "%(job_status)s")
      format_args["job_status"] = job_status
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.ModeldbUpdateJobResponse import ModeldbUpdateJobResponse
      ret = ModeldbUpdateJobResponse.from_json(ret)

    return ret
