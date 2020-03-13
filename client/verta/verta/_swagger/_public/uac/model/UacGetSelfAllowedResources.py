# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacGetSelfAllowedResources(BaseType):
  def __init__(self, actions=None):
    required = {
      "actions": False,
    }
    self.actions = actions

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacAction import UacAction


    tmp = d.get('actions', None)
    if tmp is not None:
      d['actions'] = [UacAction.from_json(tmp) for tmp in tmp]

    return UacGetSelfAllowedResources(**d)
