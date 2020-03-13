# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbDeleteExperimentArtifact(BaseType):
  def __init__(self, id=None, key=None):
    required = {
      "id": False,
      "key": False,
    }
    self.id = id
    self.key = key

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('key', None)
    if tmp is not None:
      d['key'] = tmp

    return ModeldbDeleteExperimentArtifact(**d)
