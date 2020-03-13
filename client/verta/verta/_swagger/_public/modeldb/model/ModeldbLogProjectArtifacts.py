# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbLogProjectArtifacts(BaseType):
  def __init__(self, id=None, artifacts=None):
    required = {
      "id": False,
      "artifacts": False,
    }
    self.id = id
    self.artifacts = artifacts

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .ModeldbArtifact import ModeldbArtifact


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('artifacts', None)
    if tmp is not None:
      d['artifacts'] = [ModeldbArtifact.from_json(tmp) for tmp in tmp]

    return ModeldbLogProjectArtifacts(**d)
