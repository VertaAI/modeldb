# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacGetUsersResponse(BaseType):
  def __init__(self, user_infos=None):
    required = {
      "user_infos": False,
    }
    self.user_infos = user_infos

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacUserInfo import UacUserInfo


    tmp = d.get('user_infos', None)
    if tmp is not None:
      d['user_infos'] = [UacUserInfo.from_json(tmp) for tmp in tmp]

    return UacGetUsersResponse(**d)
