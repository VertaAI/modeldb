# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacListTeamUserResponse(BaseType):
  def __init__(self, user_ids=None):
    required = {
      "user_ids": False,
    }
    self.user_ids = user_ids

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('user_ids', None)
    if tmp is not None:
      d['user_ids'] = [tmp for tmp in tmp]

    return UacListTeamUserResponse(**d)
