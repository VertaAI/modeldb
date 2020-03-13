# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbComment(BaseType):
  def __init__(self, id=None, user_id=None, date_time=None, message=None, user_info=None, verta_id=None):
    required = {
      "id": False,
      "user_id": False,
      "date_time": False,
      "message": False,
      "user_info": False,
      "verta_id": False,
    }
    self.id = id
    self.user_id = user_id
    self.date_time = date_time
    self.message = message
    self.user_info = user_info
    self.verta_id = verta_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    from .UacUserInfo import UacUserInfo

    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('user_id', None)
    if tmp is not None:
      d['user_id'] = tmp
    tmp = d.get('date_time', None)
    if tmp is not None:
      d['date_time'] = tmp
    tmp = d.get('message', None)
    if tmp is not None:
      d['message'] = tmp
    tmp = d.get('user_info', None)
    if tmp is not None:
      d['user_info'] = UacUserInfo.from_json(tmp)
    tmp = d.get('verta_id', None)
    if tmp is not None:
      d['verta_id'] = tmp

    return ModeldbComment(**d)
