# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbUpdateDatasetVersionDescriptionResponse(BaseType):
  def __init__(self, dataset_version=None):
    required = {
      "dataset_version": False,
    }
    self.dataset_version = dataset_version

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbDatasetVersion import ModeldbDatasetVersion


    tmp = d.get('dataset_version', None)
    if tmp is not None:
      d['dataset_version'] = ModeldbDatasetVersion.from_json(tmp)

    return ModeldbUpdateDatasetVersionDescriptionResponse(**d)
