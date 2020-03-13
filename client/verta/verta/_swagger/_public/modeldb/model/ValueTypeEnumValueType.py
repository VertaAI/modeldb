# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ValueTypeEnumValueType(BaseType):
  _valid_values = [
    "STRING",
    "NUMBER",
    "LIST",
    "BLOB",
  ]

  def __init__(self, val):
    if val not in ValueTypeEnumValueType._valid_values:
      raise ValueError('{} is not a valid value for ValueTypeEnumValueType'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return ValueTypeEnumValueType(v)
    else:
      return ValueTypeEnumValueType(ValueTypeEnumValueType._valid_values[v])

