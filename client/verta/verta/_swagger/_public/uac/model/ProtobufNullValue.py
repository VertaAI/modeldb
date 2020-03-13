# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ProtobufNullValue(BaseType):
  _valid_values = [
    "NULL_VALUE",
  ]

  def __init__(self, val):
    if val not in ProtobufNullValue._valid_values:
      raise ValueError('{} is not a valid value for ProtobufNullValue'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return ProtobufNullValue(v)
    else:
      return ProtobufNullValue(ProtobufNullValue._valid_values[v])

