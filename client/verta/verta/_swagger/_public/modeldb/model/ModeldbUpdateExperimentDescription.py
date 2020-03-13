# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbUpdateExperimentDescription(BaseType):
  def __init__(self, id=None, description=None):
    required = {
      "id": False,
      "description": False,
    }
    self.id = id
    self.description = description

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp

    return ModeldbUpdateExperimentDescription(**d)
