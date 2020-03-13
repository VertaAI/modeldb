# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacGetRoleByNameResponse(BaseType):
  def __init__(self, role=None):
    required = {
      "role": False,
    }
    self.role = role

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacRole import UacRole


    tmp = d.get('role', None)
    if tmp is not None:
      d['role'] = UacRole.from_json(tmp)

    return UacGetRoleByNameResponse(**d)
