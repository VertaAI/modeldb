# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacShareViaEnum(BaseType):
  _valid_values = [
    "USER_ID",
    "EMAIL_ID",
    "USERNAME",
  ]

  def __init__(self, val):
    if val not in UacShareViaEnum._valid_values:
      raise ValueError('{} is not a valid value for UacShareViaEnum'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return UacShareViaEnum(v)
    else:
      return UacShareViaEnum(UacShareViaEnum._valid_values[v])

