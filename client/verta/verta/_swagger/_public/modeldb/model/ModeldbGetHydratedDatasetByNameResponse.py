# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetHydratedDatasetByNameResponse(BaseType):
  def __init__(self, hydrated_dataset_by_user=None, shared_hydrated_datasets=None):
    required = {
      "hydrated_dataset_by_user": False,
      "shared_hydrated_datasets": False,
    }
    self.hydrated_dataset_by_user = hydrated_dataset_by_user
    self.shared_hydrated_datasets = shared_hydrated_datasets

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbHydratedDataset import ModeldbHydratedDataset

    from .ModeldbHydratedDataset import ModeldbHydratedDataset


    tmp = d.get('hydrated_dataset_by_user', None)
    if tmp is not None:
      d['hydrated_dataset_by_user'] = ModeldbHydratedDataset.from_json(tmp)
    tmp = d.get('shared_hydrated_datasets', None)
    if tmp is not None:
      d['shared_hydrated_datasets'] = [ModeldbHydratedDataset.from_json(tmp) for tmp in tmp]

    return ModeldbGetHydratedDatasetByNameResponse(**d)
