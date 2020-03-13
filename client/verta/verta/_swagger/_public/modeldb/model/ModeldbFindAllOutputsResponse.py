# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindAllOutputsResponse(BaseType):
  def __init__(self, outputs=None):
    required = {
      "outputs": False,
    }
    self.outputs = outputs

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbLineageEntryBatch import ModeldbLineageEntryBatch


    tmp = d.get('outputs', None)
    if tmp is not None:
      d['outputs'] = [ModeldbLineageEntryBatch.from_json(tmp) for tmp in tmp]

    return ModeldbFindAllOutputsResponse(**d)
