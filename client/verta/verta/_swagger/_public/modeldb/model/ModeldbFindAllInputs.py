# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindAllInputs(BaseType):
  def __init__(self, items=None):
    required = {
      "items": False,
    }
    self.items = items

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbLineageEntry import ModeldbLineageEntry


    tmp = d.get('items', None)
    if tmp is not None:
      d['items'] = [ModeldbLineageEntry.from_json(tmp) for tmp in tmp]

    return ModeldbFindAllInputs(**d)
