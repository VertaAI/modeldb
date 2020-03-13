# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacSetRoleBindingResponse(BaseType):
  def __init__(self, role_binding=None):
    required = {
      "role_binding": False,
    }
    self.role_binding = role_binding

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacRoleBinding import UacRoleBinding


    tmp = d.get('role_binding', None)
    if tmp is not None:
      d['role_binding'] = UacRoleBinding.from_json(tmp)

    return UacSetRoleBindingResponse(**d)
