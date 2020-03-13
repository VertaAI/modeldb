# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacDeleteUser(BaseType):
  def __init__(self, user_id=None):
    required = {
      "user_id": False,
    }
    self.user_id = user_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('user_id', None)
    if tmp is not None:
      d['user_id'] = tmp

    return UacDeleteUser(**d)
