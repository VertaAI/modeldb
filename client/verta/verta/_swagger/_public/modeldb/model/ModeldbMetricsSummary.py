# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbMetricsSummary(BaseType):
  def __init__(self, key=None, min_value=None, max_value=None):
    required = {
      "key": False,
      "min_value": False,
      "max_value": False,
    }
    self.key = key
    self.min_value = min_value
    self.max_value = max_value

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    

    tmp = d.get('key', None)
    if tmp is not None:
      d['key'] = tmp
    tmp = d.get('min_value', None)
    if tmp is not None:
      d['min_value'] = tmp
    tmp = d.get('max_value', None)
    if tmp is not None:
      d['max_value'] = tmp

    return ModeldbMetricsSummary(**d)
