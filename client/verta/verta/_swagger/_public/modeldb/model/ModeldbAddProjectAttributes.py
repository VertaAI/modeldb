# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbAddProjectAttributes(BaseType):
  def __init__(self, id=None, attributes=None):
    required = {
      "id": False,
      "attributes": False,
    }
    self.id = id
    self.attributes = attributes

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .CommonKeyValue import CommonKeyValue


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('attributes', None)
    if tmp is not None:
      d['attributes'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]

    return ModeldbAddProjectAttributes(**d)
