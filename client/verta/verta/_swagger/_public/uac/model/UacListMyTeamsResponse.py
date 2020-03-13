# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacListMyTeamsResponse(BaseType):
  def __init__(self, teams=None):
    required = {
      "teams": False,
    }
    self.teams = teams

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacTeam import UacTeam


    tmp = d.get('teams', None)
    if tmp is not None:
      d['teams'] = [UacTeam.from_json(tmp) for tmp in tmp]

    return UacListMyTeamsResponse(**d)
