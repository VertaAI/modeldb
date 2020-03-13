# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacGetSelfAllowedActionsBatch(BaseType):
  def __init__(self, resources=None):
    required = {
      "resources": False,
    }
    self.resources = resources

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacResources import UacResources


    tmp = d.get('resources', None)
    if tmp is not None:
      d['resources'] = UacResources.from_json(tmp)

    return UacGetSelfAllowedActionsBatch(**d)
