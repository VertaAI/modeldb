# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacDeleteTeam(BaseType):
  def __init__(self, team_id=None):
    required = {
      "team_id": False,
    }
    self.team_id = team_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('team_id', None)
    if tmp is not None:
      d['team_id'] = tmp

    return UacDeleteTeam(**d)
