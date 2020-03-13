# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbQueryParameter(BaseType):
  def __init__(self, parameter_name=None, parameter_type=None, value=None):
    required = {
      "parameter_name": False,
      "parameter_type": False,
      "value": False,
    }
    self.parameter_name = parameter_name
    self.parameter_type = parameter_type
    self.value = value

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .ValueTypeEnumValueType import ValueTypeEnumValueType

    from .ProtobufValue import ProtobufValue


    tmp = d.get('parameter_name', None)
    if tmp is not None:
      d['parameter_name'] = tmp
    tmp = d.get('parameter_type', None)
    if tmp is not None:
      d['parameter_type'] = ValueTypeEnumValueType.from_json(tmp)
    tmp = d.get('value', None)
    if tmp is not None:
      d['value'] = ProtobufValue.from_json(tmp)

    return ModeldbQueryParameter(**d)
