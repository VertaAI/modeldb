# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacListRolesResponse(BaseType):
  def __init__(self, roles=None):
    required = {
      "roles": False,
    }
    self.roles = roles

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacRole import UacRole


    tmp = d.get('roles', None)
    if tmp is not None:
      d['roles'] = [UacRole.from_json(tmp) for tmp in tmp]

    return UacListRolesResponse(**d)
