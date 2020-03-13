# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetProjectByNameResponse(BaseType):
  def __init__(self, project_by_user=None, shared_projects=None):
    required = {
      "project_by_user": False,
      "shared_projects": False,
    }
    self.project_by_user = project_by_user
    self.shared_projects = shared_projects

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbProject import ModeldbProject

    from .ModeldbProject import ModeldbProject


    tmp = d.get('project_by_user', None)
    if tmp is not None:
      d['project_by_user'] = ModeldbProject.from_json(tmp)
    tmp = d.get('shared_projects', None)
    if tmp is not None:
      d['shared_projects'] = [ModeldbProject.from_json(tmp) for tmp in tmp]

    return ModeldbGetProjectByNameResponse(**d)
