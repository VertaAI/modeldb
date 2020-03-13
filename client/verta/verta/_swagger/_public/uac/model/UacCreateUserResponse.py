# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacCreateUserResponse(BaseType):
  def __init__(self, info=None):
    required = {
      "info": False,
    }
    self.info = info

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacUserInfo import UacUserInfo


    tmp = d.get('info', None)
    if tmp is not None:
      d['info'] = UacUserInfo.from_json(tmp)

    return UacCreateUserResponse(**d)
