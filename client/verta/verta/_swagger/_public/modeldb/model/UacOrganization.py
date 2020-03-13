# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacOrganization(BaseType):
  def __init__(self, id=None, name=None, short_name=None, description=None, owner_id=None, created_timestamp=None, updated_timestamp=None, global_collaborator_type=None, global_can_deploy=None):
    required = {
      "id": False,
      "name": False,
      "short_name": False,
      "description": False,
      "owner_id": False,
      "created_timestamp": False,
      "updated_timestamp": False,
      "global_collaborator_type": False,
      "global_can_deploy": False,
    }
    self.id = id
    self.name = name
    self.short_name = short_name
    self.description = description
    self.owner_id = owner_id
    self.created_timestamp = created_timestamp
    self.updated_timestamp = updated_timestamp
    self.global_collaborator_type = global_collaborator_type
    self.global_can_deploy = global_can_deploy

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    
    
    
    from .CollaboratorTypeEnumCollaboratorType import CollaboratorTypeEnumCollaboratorType

    from .TernaryEnumTernary import TernaryEnumTernary


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
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
    tmp = d.get('global_collaborator_type', None)
    if tmp is not None:
      d['global_collaborator_type'] = CollaboratorTypeEnumCollaboratorType.from_json(tmp)
    tmp = d.get('global_can_deploy', None)
    if tmp is not None:
      d['global_can_deploy'] = TernaryEnumTernary.from_json(tmp)

    return UacOrganization(**d)
