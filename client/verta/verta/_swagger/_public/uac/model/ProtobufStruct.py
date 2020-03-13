# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ProtobufStruct(BaseType):
  def __init__(self, fields=None):
    required = {
      "fields": False,
    }
    self.fields = fields

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ProtobufValue import ProtobufValue


    tmp = d.get('fields', None)
    if tmp is not None:
      d['fields'] = {k: ProtobufValue.from_json(tmp) for k, tmp in tmp.items()}

    return ProtobufStruct(**d)
