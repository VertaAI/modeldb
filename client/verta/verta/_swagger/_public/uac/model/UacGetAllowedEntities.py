# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacGetAllowedEntities(BaseType):
  def __init__(self, actions=None, resources=None):
    required = {
      "actions": False,
      "resources": False,
    }
    self.actions = actions
    self.resources = resources

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacAction import UacAction

    from .UacResources import UacResources


    tmp = d.get('actions', None)
    if tmp is not None:
      d['actions'] = [UacAction.from_json(tmp) for tmp in tmp]
    tmp = d.get('resources', None)
    if tmp is not None:
      d['resources'] = [UacResources.from_json(tmp) for tmp in tmp]

    return UacGetAllowedEntities(**d)
