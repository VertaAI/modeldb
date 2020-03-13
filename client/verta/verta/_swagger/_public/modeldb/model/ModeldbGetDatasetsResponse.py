# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetDatasetsResponse(BaseType):
  def __init__(self, datasets=None):
    required = {
      "datasets": False,
    }
    self.datasets = datasets

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbArtifact import ModeldbArtifact


    tmp = d.get('datasets', None)
    if tmp is not None:
      d['datasets'] = [ModeldbArtifact.from_json(tmp) for tmp in tmp]

    return ModeldbGetDatasetsResponse(**d)
