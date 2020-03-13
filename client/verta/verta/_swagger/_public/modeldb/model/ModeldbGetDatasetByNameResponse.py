# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetDatasetByNameResponse(BaseType):
  def __init__(self, dataset_by_user=None, shared_datasets=None):
    required = {
      "dataset_by_user": False,
      "shared_datasets": False,
    }
    self.dataset_by_user = dataset_by_user
    self.shared_datasets = shared_datasets

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbDataset import ModeldbDataset

    from .ModeldbDataset import ModeldbDataset


    tmp = d.get('dataset_by_user', None)
    if tmp is not None:
      d['dataset_by_user'] = ModeldbDataset.from_json(tmp)
    tmp = d.get('shared_datasets', None)
    if tmp is not None:
      d['shared_datasets'] = [ModeldbDataset.from_json(tmp) for tmp in tmp]

    return ModeldbGetDatasetByNameResponse(**d)
