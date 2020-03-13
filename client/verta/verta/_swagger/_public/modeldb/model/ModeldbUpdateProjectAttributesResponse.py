# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbUpdateProjectAttributesResponse(BaseType):
  def __init__(self, project=None):
    required = {
      "project": False,
    }
    self.project = project

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbProject import ModeldbProject


    tmp = d.get('project', None)
    if tmp is not None:
      d['project'] = ModeldbProject.from_json(tmp)

    return ModeldbUpdateProjectAttributesResponse(**d)
