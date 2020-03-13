# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbKeyValueQuery(BaseType):
  def __init__(self, key=None, value=None, value_type=None, operator=None):
    required = {
      "key": False,
      "value": False,
      "value_type": False,
      "operator": False,
    }
    self.key = key
    self.value = value
    self.value_type = value_type
    self.operator = operator

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .ProtobufValue import ProtobufValue

    from .ValueTypeEnumValueType import ValueTypeEnumValueType

    from .OperatorEnumOperator import OperatorEnumOperator


    tmp = d.get('key', None)
    if tmp is not None:
      d['key'] = tmp
    tmp = d.get('value', None)
    if tmp is not None:
      d['value'] = ProtobufValue.from_json(tmp)
    tmp = d.get('value_type', None)
    if tmp is not None:
      d['value_type'] = ValueTypeEnumValueType.from_json(tmp)
    tmp = d.get('operator', None)
    if tmp is not None:
      d['operator'] = OperatorEnumOperator.from_json(tmp)

    return ModeldbKeyValueQuery(**d)
