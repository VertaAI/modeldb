# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacAddUser(BaseType):
  def __init__(self, org_id=None, share_with=None):
    required = {
      "org_id": False,
      "share_with": False,
    }
    self.org_id = org_id
    self.share_with = share_with

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('org_id', None)
    if tmp is not None:
      d['org_id'] = tmp
    tmp = d.get('share_with', None)
    if tmp is not None:
      d['share_with'] = tmp

    return UacAddUser(**d)
