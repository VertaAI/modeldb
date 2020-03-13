# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacGetAllowedEntitiesResponse(BaseType):
  def __init__(self, entities=None):
    required = {
      "entities": False,
    }
    self.entities = entities

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacEntities import UacEntities


    tmp = d.get('entities', None)
    if tmp is not None:
      d['entities'] = [UacEntities.from_json(tmp) for tmp in tmp]

    return UacGetAllowedEntitiesResponse(**d)
