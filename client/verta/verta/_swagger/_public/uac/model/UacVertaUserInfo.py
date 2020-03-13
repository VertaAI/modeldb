# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacVertaUserInfo(BaseType):
  def __init__(self, individual_user=None, username=None, refresh_timestamp=None, last_login_timestamp=None, user_id=None, publicProfile=None):
    required = {
      "individual_user": False,
      "username": False,
      "refresh_timestamp": False,
      "last_login_timestamp": False,
      "user_id": False,
      "publicProfile": False,
    }
    self.individual_user = individual_user
    self.username = username
    self.refresh_timestamp = refresh_timestamp
    self.last_login_timestamp = last_login_timestamp
    self.user_id = user_id
    self.publicProfile = publicProfile

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    
    from .UacFlagEnum import UacFlagEnum


    tmp = d.get('individual_user', None)
    if tmp is not None:
      d['individual_user'] = tmp
    tmp = d.get('username', None)
    if tmp is not None:
      d['username'] = tmp
    tmp = d.get('refresh_timestamp', None)
    if tmp is not None:
      d['refresh_timestamp'] = tmp
    tmp = d.get('last_login_timestamp', None)
    if tmp is not None:
      d['last_login_timestamp'] = tmp
    tmp = d.get('user_id', None)
    if tmp is not None:
      d['user_id'] = tmp
    tmp = d.get('publicProfile', None)
    if tmp is not None:
      d['publicProfile'] = UacFlagEnum.from_json(tmp)

    return UacVertaUserInfo(**d)
