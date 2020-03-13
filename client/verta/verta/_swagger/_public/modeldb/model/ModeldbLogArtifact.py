# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbLogArtifact(BaseType):
  def __init__(self, id=None, artifact=None):
    required = {
      "id": False,
      "artifact": False,
    }
    self.id = id
    self.artifact = artifact

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .ModeldbArtifact import ModeldbArtifact


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('artifact', None)
    if tmp is not None:
      d['artifact'] = ModeldbArtifact.from_json(tmp)

    return ModeldbLogArtifact(**d)
