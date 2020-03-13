# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbUpdateExperimentNameOrDescription(BaseType):
  def __init__(self, id=None, name=None, description=None):
    required = {
      "id": False,
      "name": False,
      "description": False,
    }
    self.id = id
    self.name = name
    self.description = description

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp

    return ModeldbUpdateExperimentNameOrDescription(**d)
