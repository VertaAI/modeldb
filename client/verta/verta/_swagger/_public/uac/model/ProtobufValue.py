# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ProtobufValue(BaseType):
  def __init__(self, null_value=None, number_value=None, string_value=None, bool_value=None, struct_value=None, list_value=None):
    required = {
      "null_value": False,
      "number_value": False,
      "string_value": False,
      "bool_value": False,
      "struct_value": False,
      "list_value": False,
    }
    self.null_value = null_value
    self.number_value = number_value
    self.string_value = string_value
    self.bool_value = bool_value
    self.struct_value = struct_value
    self.list_value = list_value

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ProtobufNullValue import ProtobufNullValue

    
    
    
    from .ProtobufStruct import ProtobufStruct

    from .ProtobufListValue import ProtobufListValue


    tmp = d.get('null_value', None)
    if tmp is not None:
      d['null_value'] = ProtobufNullValue.from_json(tmp)
    tmp = d.get('number_value', None)
    if tmp is not None:
      d['number_value'] = tmp
    tmp = d.get('string_value', None)
    if tmp is not None:
      d['string_value'] = tmp
    tmp = d.get('bool_value', None)
    if tmp is not None:
      d['bool_value'] = tmp
    tmp = d.get('struct_value', None)
    if tmp is not None:
      d['struct_value'] = ProtobufStruct.from_json(tmp)
    tmp = d.get('list_value', None)
    if tmp is not None:
      d['list_value'] = ProtobufListValue.from_json(tmp)

    return ProtobufValue(**d)
