# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetAllDatasetsResponse(BaseType):
  def __init__(self, datasets=None, total_records=None):
    required = {
      "datasets": False,
      "total_records": False,
    }
    self.datasets = datasets
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbDataset import ModeldbDataset

    

    tmp = d.get('datasets', None)
    if tmp is not None:
      d['datasets'] = [ModeldbDataset.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return ModeldbGetAllDatasetsResponse(**d)
