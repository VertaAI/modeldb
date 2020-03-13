# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacTeam(BaseType):
  def __init__(self, id=None, org_id=None, name=None, short_name=None, description=None, owner_id=None, created_timestamp=None, updated_timestamp=None):
    required = {
      "id": False,
      "org_id": False,
      "name": False,
      "short_name": False,
      "description": False,
      "owner_id": False,
      "created_timestamp": False,
      "updated_timestamp": False,
    }
    self.id = id
    self.org_id = org_id
    self.name = name
    self.short_name = short_name
    self.description = description
    self.owner_id = owner_id
    self.created_timestamp = created_timestamp
    self.updated_timestamp = updated_timestamp

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    
    
    
    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('org_id', None)
    if tmp is not None:
      d['org_id'] = tmp
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('short_name', None)
    if tmp is not None:
      d['short_name'] = tmp
    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp
    tmp = d.get('owner_id', None)
    if tmp is not None:
      d['owner_id'] = tmp
    tmp = d.get('created_timestamp', None)
    if tmp is not None:
      d['created_timestamp'] = tmp
    tmp = d.get('updated_timestamp', None)
    if tmp is not None:
      d['updated_timestamp'] = tmp

    return UacTeam(**d)
