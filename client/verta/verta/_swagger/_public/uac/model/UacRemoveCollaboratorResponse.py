# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacRemoveCollaboratorResponse(BaseType):
  def __init__(self, status=None, self_allowed_actions=None):
    required = {
      "status": False,
      "self_allowed_actions": False,
    }
    self.status = status
    self.self_allowed_actions = self_allowed_actions

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .UacAction import UacAction


    tmp = d.get('status', None)
    if tmp is not None:
      d['status'] = tmp
    tmp = d.get('self_allowed_actions', None)
    if tmp is not None:
      d['self_allowed_actions'] = [UacAction.from_json(tmp) for tmp in tmp]

    return UacRemoveCollaboratorResponse(**d)
