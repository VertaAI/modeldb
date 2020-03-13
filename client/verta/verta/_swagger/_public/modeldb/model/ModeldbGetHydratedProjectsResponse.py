# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetHydratedProjectsResponse(BaseType):
  def __init__(self, hydrated_projects=None, total_records=None):
    required = {
      "hydrated_projects": False,
      "total_records": False,
    }
    self.hydrated_projects = hydrated_projects
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbHydratedProject import ModeldbHydratedProject

    

    tmp = d.get('hydrated_projects', None)
    if tmp is not None:
      d['hydrated_projects'] = [ModeldbHydratedProject.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return ModeldbGetHydratedProjectsResponse(**d)
