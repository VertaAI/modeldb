# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbLogHyperparameter(BaseType):
  def __init__(self, id=None, hyperparameter=None):
    required = {
      "id": False,
      "hyperparameter": False,
    }
    self.id = id
    self.hyperparameter = hyperparameter

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .CommonKeyValue import CommonKeyValue


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('hyperparameter', None)
    if tmp is not None:
      d['hyperparameter'] = CommonKeyValue.from_json(tmp)

    return ModeldbLogHyperparameter(**d)
