# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetObservationsResponse(BaseType):
  def __init__(self, observations=None):
    required = {
      "observations": False,
    }
    self.observations = observations

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbObservation import ModeldbObservation


    tmp = d.get('observations', None)
    if tmp is not None:
      d['observations'] = [ModeldbObservation.from_json(tmp) for tmp in tmp]

    return ModeldbGetObservationsResponse(**d)
