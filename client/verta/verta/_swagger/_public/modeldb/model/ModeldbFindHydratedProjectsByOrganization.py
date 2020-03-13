# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindHydratedProjectsByOrganization(BaseType):
  def __init__(self, find_projects=None, name=None, id=None):
    required = {
      "find_projects": False,
      "name": False,
      "id": False,
    }
    self.find_projects = find_projects
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
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp

    return ModeldbFindHydratedProjectsByOrganization(**d)
