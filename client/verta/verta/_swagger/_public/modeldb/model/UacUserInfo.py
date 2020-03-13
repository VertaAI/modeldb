# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacUserInfo(BaseType):
  def __init__(self, user_id=None, full_name=None, first_name=None, last_name=None, email=None, id_service_provider=None, roles=None, image_url=None, dev_key=None, verta_info=None):
    required = {
      "user_id": False,
      "full_name": False,
      "first_name": False,
      "last_name": False,
      "email": False,
      "id_service_provider": False,
      "roles": False,
      "image_url": False,
      "dev_key": False,
      "verta_info": False,
    }
    self.user_id = user_id
    self.full_name = full_name
    self.first_name = first_name
    self.last_name = last_name
    self.email = email
    self.id_service_provider = id_service_provider
    self.roles = roles
    self.image_url = image_url
    self.dev_key = dev_key
    self.verta_info = verta_info

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    
    from .IdServiceProviderEnumIdServiceProvider import IdServiceProviderEnumIdServiceProvider

    
    
    
    from .UacVertaUserInfo import UacVertaUserInfo


    tmp = d.get('user_id', None)
    if tmp is not None:
      d['user_id'] = tmp
    tmp = d.get('full_name', None)
    if tmp is not None:
      d['full_name'] = tmp
    tmp = d.get('first_name', None)
    if tmp is not None:
      d['first_name'] = tmp
    tmp = d.get('last_name', None)
    if tmp is not None:
      d['last_name'] = tmp
    tmp = d.get('email', None)
    if tmp is not None:
      d['email'] = tmp
    tmp = d.get('id_service_provider', None)
    if tmp is not None:
      d['id_service_provider'] = IdServiceProviderEnumIdServiceProvider.from_json(tmp)
    tmp = d.get('roles', None)
    if tmp is not None:
      d['roles'] = [tmp for tmp in tmp]
    tmp = d.get('image_url', None)
    if tmp is not None:
      d['image_url'] = tmp
    tmp = d.get('dev_key', None)
    if tmp is not None:
      d['dev_key'] = tmp
    tmp = d.get('verta_info', None)
    if tmp is not None:
      d['verta_info'] = UacVertaUserInfo.from_json(tmp)

    return UacUserInfo(**d)
