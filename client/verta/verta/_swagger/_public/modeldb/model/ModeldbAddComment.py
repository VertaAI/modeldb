# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbAddComment(BaseType):
  def __init__(self, entity_id=None, date_time=None, message=None):
    required = {
      "entity_id": False,
      "date_time": False,
      "message": False,
    }
    self.entity_id = entity_id
    self.date_time = date_time
    self.message = message

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    

    tmp = d.get('entity_id', None)
    if tmp is not None:
      d['entity_id'] = tmp
    tmp = d.get('date_time', None)
    if tmp is not None:
      d['date_time'] = tmp
    tmp = d.get('message', None)
    if tmp is not None:
      d['message'] = tmp

    return ModeldbAddComment(**d)
