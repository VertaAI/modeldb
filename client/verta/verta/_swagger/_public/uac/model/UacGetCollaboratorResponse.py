# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacGetCollaboratorResponse(BaseType):
  def __init__(self, user_id=None, collaborator_type=None, share_via_type=None, verta_id=None, can_deploy=None, authz_entity_type=None):
    required = {
      "user_id": False,
      "collaborator_type": False,
      "share_via_type": False,
      "verta_id": False,
      "can_deploy": False,
      "authz_entity_type": False,
    }
    self.user_id = user_id
    self.collaborator_type = collaborator_type
    self.share_via_type = share_via_type
    self.verta_id = verta_id
    self.can_deploy = can_deploy
    self.authz_entity_type = authz_entity_type

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .CollaboratorTypeEnumCollaboratorType import CollaboratorTypeEnumCollaboratorType

    from .UacShareViaEnum import UacShareViaEnum

    
    from .TernaryEnumTernary import TernaryEnumTernary

    from .EntitiesEnumEntitiesTypes import EntitiesEnumEntitiesTypes


    tmp = d.get('user_id', None)
    if tmp is not None:
      d['user_id'] = tmp
    tmp = d.get('collaborator_type', None)
    if tmp is not None:
      d['collaborator_type'] = CollaboratorTypeEnumCollaboratorType.from_json(tmp)
    tmp = d.get('share_via_type', None)
    if tmp is not None:
      d['share_via_type'] = UacShareViaEnum.from_json(tmp)
    tmp = d.get('verta_id', None)
    if tmp is not None:
      d['verta_id'] = tmp
    tmp = d.get('can_deploy', None)
    if tmp is not None:
      d['can_deploy'] = TernaryEnumTernary.from_json(tmp)
    tmp = d.get('authz_entity_type', None)
    if tmp is not None:
      d['authz_entity_type'] = EntitiesEnumEntitiesTypes.from_json(tmp)

    return UacGetCollaboratorResponse(**d)
