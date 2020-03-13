# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacEntities(BaseType):
  def __init__(self, user_ids=None, org_ids=None, team_ids=None):
    required = {
      "user_ids": False,
      "org_ids": False,
      "team_ids": False,
    }
    self.user_ids = user_ids
    self.org_ids = org_ids
    self.team_ids = team_ids

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    

    tmp = d.get('user_ids', None)
    if tmp is not None:
      d['user_ids'] = [tmp for tmp in tmp]
    tmp = d.get('org_ids', None)
    if tmp is not None:
      d['org_ids'] = [tmp for tmp in tmp]
    tmp = d.get('team_ids', None)
    if tmp is not None:
      d['team_ids'] = [tmp for tmp in tmp]

    return UacEntities(**d)
