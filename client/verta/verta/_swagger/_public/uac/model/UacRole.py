# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacRole(BaseType):
  def __init__(self, id=None, name=None, scope=None, resource_action_groups=None):
    required = {
      "id": False,
      "name": False,
      "scope": False,
      "resource_action_groups": False,
    }
    self.id = id
    self.name = name
    self.scope = scope
    self.resource_action_groups = resource_action_groups

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    from .UacRoleScope import UacRoleScope

    from .UacResourceActionGroup import UacResourceActionGroup


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('scope', None)
    if tmp is not None:
      d['scope'] = UacRoleScope.from_json(tmp)
    tmp = d.get('resource_action_groups', None)
    if tmp is not None:
      d['resource_action_groups'] = [UacResourceActionGroup.from_json(tmp) for tmp in tmp]

    return UacRole(**d)
