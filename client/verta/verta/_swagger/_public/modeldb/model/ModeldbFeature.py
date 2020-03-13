# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFeature(BaseType):
  def __init__(self, name=None):
    required = {
      "name": False,
    }
    self.name = name

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp

    return ModeldbFeature(**d)
