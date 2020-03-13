# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbCollaboratorUserInfo(BaseType):
  def __init__(self, collaborator_user_info=None, collaborator_organization=None, collaborator_team=None, collaborator_type=None, can_deploy=None, entity_type=None):
    required = {
      "collaborator_user_info": False,
      "collaborator_organization": False,
      "collaborator_team": False,
      "collaborator_type": False,
      "can_deploy": False,
      "entity_type": False,
    }
    self.collaborator_user_info = collaborator_user_info
    self.collaborator_organization = collaborator_organization
    self.collaborator_team = collaborator_team
    self.collaborator_type = collaborator_type
    self.can_deploy = can_deploy
    self.entity_type = entity_type

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacUserInfo import UacUserInfo

    from .UacOrganization import UacOrganization

    from .UacTeam import UacTeam

    from .CollaboratorTypeEnumCollaboratorType import CollaboratorTypeEnumCollaboratorType

    from .TernaryEnumTernary import TernaryEnumTernary

    from .EntitiesEnumEntitiesTypes import EntitiesEnumEntitiesTypes


    tmp = d.get('collaborator_user_info', None)
    if tmp is not None:
      d['collaborator_user_info'] = UacUserInfo.from_json(tmp)
    tmp = d.get('collaborator_organization', None)
    if tmp is not None:
      d['collaborator_organization'] = UacOrganization.from_json(tmp)
    tmp = d.get('collaborator_team', None)
    if tmp is not None:
      d['collaborator_team'] = UacTeam.from_json(tmp)
    tmp = d.get('collaborator_type', None)
    if tmp is not None:
      d['collaborator_type'] = CollaboratorTypeEnumCollaboratorType.from_json(tmp)
    tmp = d.get('can_deploy', None)
    if tmp is not None:
      d['can_deploy'] = TernaryEnumTernary.from_json(tmp)
    tmp = d.get('entity_type', None)
    if tmp is not None:
      d['entity_type'] = EntitiesEnumEntitiesTypes.from_json(tmp)

    return ModeldbCollaboratorUserInfo(**d)
