# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class TernaryEnumTernary(BaseType):
  _valid_values = [
    "UNKNOWN",
    "TRUE",
    "FALSE",
  ]

  def __init__(self, val):
    if val not in TernaryEnumTernary._valid_values:
      raise ValueError('{} is not a valid value for TernaryEnumTernary'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return TernaryEnumTernary(v)
    else:
      return TernaryEnumTernary(TernaryEnumTernary._valid_values[v])

