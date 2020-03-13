# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbAdvancedQueryDatasetVersionsResponse(BaseType):
  def __init__(self, hydrated_dataset_versions=None, total_records=None):
    required = {
      "hydrated_dataset_versions": False,
      "total_records": False,
    }
    self.hydrated_dataset_versions = hydrated_dataset_versions
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbHydratedDatasetVersion import ModeldbHydratedDatasetVersion

    

    tmp = d.get('hydrated_dataset_versions', None)
    if tmp is not None:
      d['hydrated_dataset_versions'] = [ModeldbHydratedDatasetVersion.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return ModeldbAdvancedQueryDatasetVersionsResponse(**d)
