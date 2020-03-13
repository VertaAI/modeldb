# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbSetDatasetVisibilty(BaseType):
  def __init__(self, id=None, dataset_visibility=None):
    required = {
      "id": False,
      "dataset_visibility": False,
    }
    self.id = id
    self.dataset_visibility = dataset_visibility

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .DatasetVisibilityEnumDatasetVisibility import DatasetVisibilityEnumDatasetVisibility


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('dataset_visibility', None)
    if tmp is not None:
      d['dataset_visibility'] = DatasetVisibilityEnumDatasetVisibility.from_json(tmp)

    return ModeldbSetDatasetVisibilty(**d)
