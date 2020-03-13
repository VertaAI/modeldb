# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindHydratedProjectsByTeam(BaseType):
  def __init__(self, find_projects=None, org_id=None, name=None, id=None):
    required = {
      "find_projects": False,
      "org_id": False,
      "name": False,
      "id": False,
    }
    self.find_projects = find_projects
    self.org_id = org_id
    self.name = name
    self.id = id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbFindProjects import ModeldbFindProjects

    
    
    

    tmp = d.get('find_projects', None)
    if tmp is not None:
      d['find_projects'] = ModeldbFindProjects.from_json(tmp)
    tmp = d.get('org_id', None)
    if tmp is not None:
      d['org_id'] = tmp
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp

    return ModeldbFindHydratedProjectsByTeam(**d)
