# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbSetProjectVisibilty(BaseType):
  def __init__(self, id=None, project_visibility=None):
    required = {
      "id": False,
      "project_visibility": False,
    }
    self.id = id
    self.project_visibility = project_visibility

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .ModeldbProjectVisibility import ModeldbProjectVisibility


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('project_visibility', None)
    if tmp is not None:
      d['project_visibility'] = ModeldbProjectVisibility.from_json(tmp)

    return ModeldbSetProjectVisibilty(**d)
