# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbLastModifiedExperimentRunSummary(BaseType):
  def __init__(self, name=None, last_updated_time=None):
    required = {
      "name": False,
      "last_updated_time": False,
    }
    self.name = name
    self.last_updated_time = last_updated_time

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('last_updated_time', None)
    if tmp is not None:
      d['last_updated_time'] = tmp

    return ModeldbLastModifiedExperimentRunSummary(**d)
