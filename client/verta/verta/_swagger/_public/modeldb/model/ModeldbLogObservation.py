# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbLogObservation(BaseType):
  def __init__(self, id=None, observation=None):
    required = {
      "id": False,
      "observation": False,
    }
    self.id = id
    self.observation = observation

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .ModeldbObservation import ModeldbObservation


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('observation', None)
    if tmp is not None:
      d['observation'] = ModeldbObservation.from_json(tmp)

    return ModeldbLogObservation(**d)
