# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbAddAttributes(BaseType):
  def __init__(self, id=None, attribute=None):
    required = {
      "id": False,
      "attribute": False,
    }
    self.id = id
    self.attribute = attribute

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .CommonKeyValue import CommonKeyValue


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('attribute', None)
    if tmp is not None:
      d['attribute'] = CommonKeyValue.from_json(tmp)

    return ModeldbAddAttributes(**d)
