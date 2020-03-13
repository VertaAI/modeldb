# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbDeleteExperiments(BaseType):
  def __init__(self, ids=None):
    required = {
      "ids": False,
    }
    self.ids = ids

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('ids', None)
    if tmp is not None:
      d['ids'] = [tmp for tmp in tmp]

    return ModeldbDeleteExperiments(**d)
