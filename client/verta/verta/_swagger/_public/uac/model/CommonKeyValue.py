# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class CommonKeyValue(BaseType):
  def __init__(self, key=None, value=None, value_type=None):
    required = {
      "key": False,
      "value": False,
      "value_type": False,
    }
    self.key = key
    self.value = value
    self.value_type = value_type

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .ProtobufValue import ProtobufValue

    from .ValueTypeEnumValueType import ValueTypeEnumValueType


    tmp = d.get('key', None)
    if tmp is not None:
      d['key'] = tmp
    tmp = d.get('value', None)
    if tmp is not None:
      d['value'] = ProtobufValue.from_json(tmp)
    tmp = d.get('value_type', None)
    if tmp is not None:
      d['value_type'] = ValueTypeEnumValueType.from_json(tmp)

    return CommonKeyValue(**d)
