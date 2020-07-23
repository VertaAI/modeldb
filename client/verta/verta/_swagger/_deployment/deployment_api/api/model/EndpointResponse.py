# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class EndpointResponse(BaseType):
  def __init__(self, creator_request=None, date_created=None, date_updated=None, id=None, owner_id=None):
    required = {
      "creator_request": False,
      "date_created": False,
      "date_updated": False,
      "id": False,
      "owner_id": False,
    }
    self.creator_request = creator_request
    self.date_created = date_created
    self.date_updated = date_updated
    self.id = id
    self.owner_id = owner_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .EndpointCreate import EndpointCreate

    
    
    
    

    tmp = d.get('creator_request', None)
    if tmp is not None:
      d['creator_request'] = EndpointCreate.from_json(tmp)
    tmp = d.get('date_created', None)
    if tmp is not None:
      d['date_created'] = tmp
    tmp = d.get('date_updated', None)
    if tmp is not None:
      d['date_updated'] = tmp
    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('owner_id', None)
    if tmp is not None:
      d['owner_id'] = tmp

    return EndpointResponse(**d)
