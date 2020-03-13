# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ProtobufListValue(BaseType):
  def __init__(self, values=None):
    required = {
      "values": False,
    }
    self.values = values

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ProtobufValue import ProtobufValue


    tmp = d.get('values', None)
    if tmp is not None:
      d['values'] = [ProtobufValue.from_json(tmp) for tmp in tmp]

    return ProtobufListValue(**d)
