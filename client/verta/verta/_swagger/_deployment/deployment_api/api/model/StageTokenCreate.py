# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class StageTokenCreate(BaseType):
  def __init__(self, value=None):
    required = {
      "value": False,
    }
    self.value = value

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('value', None)
    if tmp is not None:
      d['value'] = tmp

    return StageTokenCreate(**d)
