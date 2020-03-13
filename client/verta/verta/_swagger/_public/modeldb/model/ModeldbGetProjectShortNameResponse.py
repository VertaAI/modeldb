# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetProjectShortNameResponse(BaseType):
  def __init__(self, short_name=None):
    required = {
      "short_name": False,
    }
    self.short_name = short_name

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('short_name', None)
    if tmp is not None:
      d['short_name'] = tmp

    return ModeldbGetProjectShortNameResponse(**d)
