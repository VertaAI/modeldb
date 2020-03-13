# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbUpdateComment(BaseType):
  def __init__(self, id=None, entity_id=None, date_time=None, message=None):
    required = {
      "id": False,
      "entity_id": False,
      "date_time": False,
      "message": False,
    }
    self.id = id
    self.entity_id = entity_id
    self.date_time = date_time
    self.message = message

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('entity_id', None)
    if tmp is not None:
      d['entity_id'] = tmp
    tmp = d.get('date_time', None)
    if tmp is not None:
      d['date_time'] = tmp
    tmp = d.get('message', None)
    if tmp is not None:
      d['message'] = tmp

    return ModeldbUpdateComment(**d)
