# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacAddCollaboratorRequestResponse(BaseType):
  def __init__(self, self_allowed_actions=None, status=None, collaborator_user_info=None, collaborator_organization=None, collaborator_team=None):
    required = {
      "self_allowed_actions": False,
      "status": False,
      "collaborator_user_info": False,
      "collaborator_organization": False,
      "collaborator_team": False,
    }
    self.self_allowed_actions = self_allowed_actions
    self.status = status
    self.collaborator_user_info = collaborator_user_info
    self.collaborator_organization = collaborator_organization
    self.collaborator_team = collaborator_team

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacAction import UacAction

    
    from .UacUserInfo import UacUserInfo

    from .UacOrganization import UacOrganization

    from .UacTeam import UacTeam


    tmp = d.get('self_allowed_actions', None)
    if tmp is not None:
      d['self_allowed_actions'] = [UacAction.from_json(tmp) for tmp in tmp]
    tmp = d.get('status', None)
    if tmp is not None:
      d['status'] = tmp
    tmp = d.get('collaborator_user_info', None)
    if tmp is not None:
      d['collaborator_user_info'] = UacUserInfo.from_json(tmp)
    tmp = d.get('collaborator_organization', None)
    if tmp is not None:
      d['collaborator_organization'] = UacOrganization.from_json(tmp)
    tmp = d.get('collaborator_team', None)
    if tmp is not None:
      d['collaborator_team'] = UacTeam.from_json(tmp)

    return UacAddCollaboratorRequestResponse(**d)
