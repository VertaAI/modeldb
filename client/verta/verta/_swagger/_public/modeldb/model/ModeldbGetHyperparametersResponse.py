# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetHyperparametersResponse(BaseType):
  def __init__(self, hyperparameters=None):
    required = {
      "hyperparameters": False,
    }
    self.hyperparameters = hyperparameters

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .CommonKeyValue import CommonKeyValue


    tmp = d.get('hyperparameters', None)
    if tmp is not None:
      d['hyperparameters'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]

    return ModeldbGetHyperparametersResponse(**d)
