# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbSetDatasetVisibiltyResponse(BaseType):
  def __init__(self, dataset=None):
    required = {
      "dataset": False,
    }
    self.dataset = dataset

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbDataset import ModeldbDataset


    tmp = d.get('dataset', None)
    if tmp is not None:
      d['dataset'] = ModeldbDataset.from_json(tmp)

    return ModeldbSetDatasetVisibiltyResponse(**d)
