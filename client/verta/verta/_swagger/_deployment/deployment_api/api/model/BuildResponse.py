# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class BuildResponse(BaseType):
  def __init__(self, creator_request=None, date_created=None, date_updated=None, id=None, message=None, status=None):
    required = {
      "creator_request": False,
      "date_created": False,
      "date_updated": False,
      "id": False,
      "message": False,
      "status": False,
    }
    self.creator_request = creator_request
    self.date_created = date_created
    self.date_updated = date_updated
    self.id = id
    self.message = message
    self.status = status

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .BuildCreate import BuildCreate

    
    
    
    
    

    tmp = d.get('creator_request', None)
    if tmp is not None:
      d['creator_request'] = 
BuildCreate.from_json(tmp)

    tmp = d.get('date_created', None)
    if tmp is not None:
      d['date_created'] = 
tmp

    tmp = d.get('date_updated', None)
    if tmp is not None:
      d['date_updated'] = 
tmp

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = 
tmp

    tmp = d.get('message', None)
    if tmp is not None:
      d['message'] = 
tmp

    tmp = d.get('status', None)
    if tmp is not None:
      d['status'] = 
tmp


    return BuildResponse(**d)
