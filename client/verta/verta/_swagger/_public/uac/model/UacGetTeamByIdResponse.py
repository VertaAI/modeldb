# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacGetTeamByIdResponse(BaseType):
  def __init__(self, team=None):
    required = {
      "team": False,
    }
    self.team = team

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacTeam import UacTeam


    tmp = d.get('team', None)
    if tmp is not None:
      d['team'] = UacTeam.from_json(tmp)

    return UacGetTeamByIdResponse(**d)
