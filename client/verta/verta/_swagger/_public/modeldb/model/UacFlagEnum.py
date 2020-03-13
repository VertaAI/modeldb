# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacFlagEnum(BaseType):
  _valid_values = [
    "UNDEFINED",
    "TRUE",
    "FALSE",
  ]

  def __init__(self, val):
    if val not in UacFlagEnum._valid_values:
      raise ValueError('{} is not a valid value for UacFlagEnum'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return UacFlagEnum(v)
    else:
      return UacFlagEnum(UacFlagEnum._valid_values[v])

