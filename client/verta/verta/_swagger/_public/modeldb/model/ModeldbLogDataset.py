# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbLogDataset(BaseType):
  def __init__(self, id=None, dataset=None, overwrite=None):
    required = {
      "id": False,
      "dataset": False,
      "overwrite": False,
    }
    self.id = id
    self.dataset = dataset
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
    tmp = d.get('dataset', None)
    if tmp is not None:
      d['dataset'] = ModeldbArtifact.from_json(tmp)
    tmp = d.get('overwrite', None)
    if tmp is not None:
      d['overwrite'] = tmp

    return ModeldbLogDataset(**d)
