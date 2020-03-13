# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacListRoleBindingsResponse(BaseType):
  def __init__(self, role_bindings=None):
    required = {
      "role_bindings": False,
    }
    self.role_bindings = role_bindings

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacRoleBinding import UacRoleBinding


    tmp = d.get('role_bindings', None)
    if tmp is not None:
      d['role_bindings'] = [UacRoleBinding.from_json(tmp) for tmp in tmp]

    return UacListRoleBindingsResponse(**d)
