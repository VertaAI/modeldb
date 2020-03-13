# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacAddCollaboratorRequest(BaseType):
  def __init__(self, entity_ids=None, share_with=None, collaborator_type=None, message=None, date_created=None, date_updated=None, can_deploy=None, authz_entity_type=None):
    required = {
      "entity_ids": False,
      "share_with": False,
      "collaborator_type": False,
      "message": False,
      "date_created": False,
      "date_updated": False,
      "can_deploy": False,
      "authz_entity_type": False,
    }
    self.entity_ids = entity_ids
    self.share_with = share_with
    self.collaborator_type = collaborator_type
    self.message = message
    self.date_created = date_created
    self.date_updated = date_updated
    self.can_deploy = can_deploy
    self.authz_entity_type = authz_entity_type

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    from .CollaboratorTypeEnumCollaboratorType import CollaboratorTypeEnumCollaboratorType

    
    
    
    from .TernaryEnumTernary import TernaryEnumTernary

    from .EntitiesEnumEntitiesTypes import EntitiesEnumEntitiesTypes


    tmp = d.get('entity_ids', None)
    if tmp is not None:
      d['entity_ids'] = [tmp for tmp in tmp]
    tmp = d.get('share_with', None)
    if tmp is not None:
      d['share_with'] = tmp
    tmp = d.get('collaborator_type', None)
    if tmp is not None:
      d['collaborator_type'] = CollaboratorTypeEnumCollaboratorType.from_json(tmp)
    tmp = d.get('message', None)
    if tmp is not None:
      d['message'] = tmp
    tmp = d.get('date_created', None)
    if tmp is not None:
      d['date_created'] = tmp
    tmp = d.get('date_updated', None)
    if tmp is not None:
      d['date_updated'] = tmp
    tmp = d.get('can_deploy', None)
    if tmp is not None:
      d['can_deploy'] = TernaryEnumTernary.from_json(tmp)
    tmp = d.get('authz_entity_type', None)
    if tmp is not None:
      d['authz_entity_type'] = EntitiesEnumEntitiesTypes.from_json(tmp)

    return UacAddCollaboratorRequest(**d)
