# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacGetUsers(BaseType):
  def __init__(self, user_ids=None, emails=None, usernames=None):
    required = {
      "user_ids": False,
      "emails": False,
      "usernames": False,
    }
    self.user_ids = user_ids
    self.emails = emails
    self.usernames = usernames

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    

    tmp = d.get('user_ids', None)
    if tmp is not None:
      d['user_ids'] = [tmp for tmp in tmp]
    tmp = d.get('emails', None)
    if tmp is not None:
      d['emails'] = [tmp for tmp in tmp]
    tmp = d.get('usernames', None)
    if tmp is not None:
      d['usernames'] = [tmp for tmp in tmp]

    return UacGetUsers(**d)
