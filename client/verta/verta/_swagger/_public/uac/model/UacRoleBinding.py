# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacRoleBinding(BaseType):
  def __init__(self, id=None, name=None, scope=None, entities=None, resources=None, role_id=None):
    required = {
      "id": False,
      "name": False,
      "scope": False,
      "entities": False,
      "resources": False,
      "role_id": False,
    }
    self.id = id
    self.name = name
    self.scope = scope
    self.entities = entities
    self.resources = resources
    self.role_id = role_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    from .UacRoleScope import UacRoleScope

    from .UacEntities import UacEntities

    from .UacResources import UacResources

    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('scope', None)
    if tmp is not None:
      d['scope'] = UacRoleScope.from_json(tmp)
    tmp = d.get('entities', None)
    if tmp is not None:
      d['entities'] = [UacEntities.from_json(tmp) for tmp in tmp]
    tmp = d.get('resources', None)
    if tmp is not None:
      d['resources'] = [UacResources.from_json(tmp) for tmp in tmp]
    tmp = d.get('role_id', None)
    if tmp is not None:
      d['role_id'] = tmp

    return UacRoleBinding(**d)
