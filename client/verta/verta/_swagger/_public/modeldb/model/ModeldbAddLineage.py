# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbAddLineage(BaseType):
  def __init__(self, input=None, output=None):
    required = {
      "input": False,
      "output": False,
    }
    self.input = input
    self.output = output

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbLineageEntry import ModeldbLineageEntry

    from .ModeldbLineageEntry import ModeldbLineageEntry


    tmp = d.get('input', None)
    if tmp is not None:
      d['input'] = [ModeldbLineageEntry.from_json(tmp) for tmp in tmp]
    tmp = d.get('output', None)
    if tmp is not None:
      d['output'] = [ModeldbLineageEntry.from_json(tmp) for tmp in tmp]

    return ModeldbAddLineage(**d)
