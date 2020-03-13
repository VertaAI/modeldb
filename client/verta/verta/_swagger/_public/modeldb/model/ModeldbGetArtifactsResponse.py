# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetArtifactsResponse(BaseType):
  def __init__(self, artifacts=None):
    required = {
      "artifacts": False,
    }
    self.artifacts = artifacts

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbArtifact import ModeldbArtifact


    tmp = d.get('artifacts', None)
    if tmp is not None:
      d['artifacts'] = [ModeldbArtifact.from_json(tmp) for tmp in tmp]

    return ModeldbGetArtifactsResponse(**d)
