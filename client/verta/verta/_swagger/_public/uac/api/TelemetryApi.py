# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class TelemetryApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def collectTelemetry(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/telemetry/collectTelemetry"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacCollectTelemetryResponse import UacCollectTelemetryResponse
      ret = UacCollectTelemetryResponse.from_json(ret)

    return ret
