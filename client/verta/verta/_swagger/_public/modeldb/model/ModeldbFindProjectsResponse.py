# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindProjectsResponse(BaseType):
  def __init__(self, projects=None, total_records=None):
    required = {
      "projects": False,
      "total_records": False,
    }
    self.projects = projects
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbProject import ModeldbProject

    

    tmp = d.get('projects', None)
    if tmp is not None:
      d['projects'] = [ModeldbProject.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return ModeldbFindProjectsResponse(**d)
