# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetExperimentRunByDataset(BaseType):
  def __init__(self, dataset_id=None):
    required = {
      "dataset_id": False,
    }
    self.dataset_id = dataset_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('dataset_id', None)
    if tmp is not None:
      d['dataset_id'] = tmp

    return ModeldbGetExperimentRunByDataset(**d)
