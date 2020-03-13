# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindAllInputsResponse(BaseType):
  def __init__(self, inputs=None):
    required = {
      "inputs": False,
    }
    self.inputs = inputs

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbLineageEntryBatch import ModeldbLineageEntryBatch


    tmp = d.get('inputs', None)
    if tmp is not None:
      d['inputs'] = [ModeldbLineageEntryBatch.from_json(tmp) for tmp in tmp]

    return ModeldbFindAllInputsResponse(**d)
