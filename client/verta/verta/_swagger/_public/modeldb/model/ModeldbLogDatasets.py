# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbLogDatasets(BaseType):
  def __init__(self, id=None, datasets=None, overwrite=None):
    required = {
      "id": False,
      "datasets": False,
      "overwrite": False,
    }
    self.id = id
    self.datasets = datasets
    self.overwrite = overwrite

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .ModeldbArtifact import ModeldbArtifact

    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('datasets', None)
    if tmp is not None:
      d['datasets'] = [ModeldbArtifact.from_json(tmp) for tmp in tmp]
    tmp = d.get('overwrite', None)
    if tmp is not None:
      d['overwrite'] = tmp

    return ModeldbLogDatasets(**d)
