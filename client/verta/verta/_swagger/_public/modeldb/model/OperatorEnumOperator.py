# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class OperatorEnumOperator(BaseType):
  _valid_values = [
    "EQ",
    "NE",
    "GT",
    "GTE",
    "LT",
    "LTE",
    "CONTAIN",
    "NOT_CONTAIN",
    "IN",
  ]

  def __init__(self, val):
    if val not in OperatorEnumOperator._valid_values:
      raise ValueError('{} is not a valid value for OperatorEnumOperator'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return OperatorEnumOperator(v)
    else:
      return OperatorEnumOperator(OperatorEnumOperator._valid_values[v])

