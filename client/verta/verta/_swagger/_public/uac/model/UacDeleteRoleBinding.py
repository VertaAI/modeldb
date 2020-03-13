# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacDeleteRoleBinding(BaseType):
  def __init__(self, id=None):
    required = {
      "id": False,
    }
    self.id = id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp

    return UacDeleteRoleBinding(**d)
