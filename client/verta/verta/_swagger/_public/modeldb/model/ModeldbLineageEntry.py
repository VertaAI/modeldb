# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbLineageEntry(BaseType):
  def __init__(self, `type`=None, external_id=None):
    required = {
      "`type`": False,
      "external_id": False,
    }
    self.`type` = `type`
    self.external_id = external_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .LineageEntryEnumLineageEntryType import LineageEntryEnumLineageEntryType

    

    tmp = d.get('`type`', None)
    if tmp is not None:
      d['`type`'] = LineageEntryEnumLineageEntryType.from_json(tmp)
    tmp = d.get('external_id', None)
    if tmp is not None:
      d['external_id'] = tmp

    return ModeldbLineageEntry(**d)
