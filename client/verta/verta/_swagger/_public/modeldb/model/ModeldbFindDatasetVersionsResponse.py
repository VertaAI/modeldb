# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindDatasetVersionsResponse(BaseType):
  def __init__(self, dataset_versions=None, total_records=None):
    required = {
      "dataset_versions": False,
      "total_records": False,
    }
    self.dataset_versions = dataset_versions
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbDatasetVersion import ModeldbDatasetVersion

    

    tmp = d.get('dataset_versions', None)
    if tmp is not None:
      d['dataset_versions'] = [ModeldbDatasetVersion.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return ModeldbFindDatasetVersionsResponse(**d)
