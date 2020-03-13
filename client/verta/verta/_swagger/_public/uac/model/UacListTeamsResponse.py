# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacListTeamsResponse(BaseType):
  def __init__(self, team_ids=None):
    required = {
      "team_ids": False,
    }
    self.team_ids = team_ids

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('team_ids', None)
    if tmp is not None:
      d['team_ids'] = [tmp for tmp in tmp]

    return UacListTeamsResponse(**d)
