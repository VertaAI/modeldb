# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacRemoveTeamUser(BaseType):
  def __init__(self, team_id=None, share_with=None):
    required = {
      "team_id": False,
      "share_with": False,
    }
    self.team_id = team_id
    self.share_with = share_with

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('team_id', None)
    if tmp is not None:
      d['team_id'] = tmp
    tmp = d.get('share_with', None)
    if tmp is not None:
      d['share_with'] = tmp

    return UacRemoveTeamUser(**d)
