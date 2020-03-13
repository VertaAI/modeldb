# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetHydratedProjectByIdResponse(BaseType):
  def __init__(self, hydrated_project=None):
    required = {
      "hydrated_project": False,
    }
    self.hydrated_project = hydrated_project

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbHydratedProject import ModeldbHydratedProject


    tmp = d.get('hydrated_project', None)
    if tmp is not None:
      d['hydrated_project'] = ModeldbHydratedProject.from_json(tmp)

    return ModeldbGetHydratedProjectByIdResponse(**d)
