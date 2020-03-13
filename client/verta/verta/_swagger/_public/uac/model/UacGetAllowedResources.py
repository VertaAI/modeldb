# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacGetAllowedResources(BaseType):
  def __init__(self, entities=None, actions=None):
    required = {
      "entities": False,
      "actions": False,
    }
    self.entities = entities
    self.actions = actions

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacEntities import UacEntities

    from .UacAction import UacAction


    tmp = d.get('entities', None)
    if tmp is not None:
      d['entities'] = [UacEntities.from_json(tmp) for tmp in tmp]
    tmp = d.get('actions', None)
    if tmp is not None:
      d['actions'] = [UacAction.from_json(tmp) for tmp in tmp]

    return UacGetAllowedResources(**d)
