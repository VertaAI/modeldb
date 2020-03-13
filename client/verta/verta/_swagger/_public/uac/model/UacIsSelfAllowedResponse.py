# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacIsSelfAllowedResponse(BaseType):
  def __init__(self, allowed=None):
    required = {
      "allowed": False,
    }
    self.allowed = allowed

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('allowed', None)
    if tmp is not None:
      d['allowed'] = tmp

    return UacIsSelfAllowedResponse(**d)
