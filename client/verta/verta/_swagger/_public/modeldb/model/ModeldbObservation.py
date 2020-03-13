# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbObservation(BaseType):
  def __init__(self, attribute=None, artifact=None, timestamp=None):
    required = {
      "attribute": False,
      "artifact": False,
      "timestamp": False,
    }
    self.attribute = attribute
    self.artifact = artifact
    self.timestamp = timestamp

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .CommonKeyValue import CommonKeyValue

    from .ModeldbArtifact import ModeldbArtifact

    

    tmp = d.get('attribute', None)
    if tmp is not None:
      d['attribute'] = CommonKeyValue.from_json(tmp)
    tmp = d.get('artifact', None)
    if tmp is not None:
      d['artifact'] = ModeldbArtifact.from_json(tmp)
    tmp = d.get('timestamp', None)
    if tmp is not None:
      d['timestamp'] = tmp

    return ModeldbObservation(**d)
