# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindHydratedProjectsByUser(BaseType):
  def __init__(self, find_projects=None, email=None, username=None, verta_id=None):
    required = {
      "find_projects": False,
      "email": False,
      "username": False,
      "verta_id": False,
    }
    self.find_projects = find_projects
    self.email = email
    self.username = username
    self.verta_id = verta_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbFindProjects import ModeldbFindProjects

    
    
    

    tmp = d.get('find_projects', None)
    if tmp is not None:
      d['find_projects'] = ModeldbFindProjects.from_json(tmp)
    tmp = d.get('email', None)
    if tmp is not None:
      d['email'] = tmp
    tmp = d.get('username', None)
    if tmp is not None:
      d['username'] = tmp
    tmp = d.get('verta_id', None)
    if tmp is not None:
      d['verta_id'] = tmp

    return ModeldbFindHydratedProjectsByUser(**d)
