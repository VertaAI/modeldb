# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetPublicProjectsResponse(BaseType):
  def __init__(self, projects=None):
    required = {
      "projects": False,
    }
    self.projects = projects

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbProject import ModeldbProject


    tmp = d.get('projects', None)
    if tmp is not None:
      d['projects'] = [ModeldbProject.from_json(tmp) for tmp in tmp]

    return ModeldbGetPublicProjectsResponse(**d)
