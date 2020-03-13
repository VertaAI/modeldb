# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacRoleScope(BaseType):
  def __init__(self, org_id=None, team_id=None):
    required = {
      "org_id": False,
      "team_id": False,
    }
    self.org_id = org_id
    self.team_id = team_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('org_id', None)
    if tmp is not None:
      d['org_id'] = tmp
    tmp = d.get('team_id', None)
    if tmp is not None:
      d['team_id'] = tmp

    return UacRoleScope(**d)
