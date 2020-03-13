# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbSetDatasetVersionVisibilty(BaseType):
  def __init__(self, id=None, dataset_version_visibility=None):
    required = {
      "id": False,
      "dataset_version_visibility": False,
    }
    self.id = id
    self.dataset_version_visibility = dataset_version_visibility

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .DatasetVisibilityEnumDatasetVisibility import DatasetVisibilityEnumDatasetVisibility


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('dataset_version_visibility', None)
    if tmp is not None:
      d['dataset_version_visibility'] = DatasetVisibilityEnumDatasetVisibility.from_json(tmp)

    return ModeldbSetDatasetVersionVisibilty(**d)
