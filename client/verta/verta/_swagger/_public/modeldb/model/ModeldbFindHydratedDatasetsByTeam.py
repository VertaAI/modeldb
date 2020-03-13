# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindHydratedDatasetsByTeam(BaseType):
  def __init__(self, find_datasets=None, org_id=None, name=None, id=None):
    required = {
      "find_datasets": False,
      "org_id": False,
      "name": False,
      "id": False,
    }
    self.find_datasets = find_datasets
    self.org_id = org_id
    self.name = name
    self.id = id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbFindDatasets import ModeldbFindDatasets

    
    
    

    tmp = d.get('find_datasets', None)
    if tmp is not None:
      d['find_datasets'] = ModeldbFindDatasets.from_json(tmp)
    tmp = d.get('org_id', None)
    if tmp is not None:
      d['org_id'] = tmp
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp

    return ModeldbFindHydratedDatasetsByTeam(**d)
