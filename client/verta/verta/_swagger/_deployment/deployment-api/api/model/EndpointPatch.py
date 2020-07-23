# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class EndpointPatch(BaseType):
  def __init__(self, description=None, path=None):
    required = {
      "description": False,
      "path": False,
    }
    self.description = description
    self.path = path

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp
    tmp = d.get('path', None)
    if tmp is not None:
      d['path'] = tmp

    return EndpointPatch(**d)
