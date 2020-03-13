# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbAdvancedQueryDatasetsResponse(BaseType):
  def __init__(self, hydrated_datasets=None, total_records=None):
    required = {
      "hydrated_datasets": False,
      "total_records": False,
    }
    self.hydrated_datasets = hydrated_datasets
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbHydratedDataset import ModeldbHydratedDataset

    

    tmp = d.get('hydrated_datasets', None)
    if tmp is not None:
      d['hydrated_datasets'] = [ModeldbHydratedDataset.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return ModeldbAdvancedQueryDatasetsResponse(**d)
