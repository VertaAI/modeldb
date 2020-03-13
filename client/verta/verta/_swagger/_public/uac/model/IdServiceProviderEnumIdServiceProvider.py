# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class IdServiceProviderEnumIdServiceProvider(BaseType):
  _valid_values = [
    "UNKNOWN",
    "GITHUB",
    "BITBUCKET",
    "GOOGLE",
    "VERTA",
  ]

  def __init__(self, val):
    if val not in IdServiceProviderEnumIdServiceProvider._valid_values:
      raise ValueError('{} is not a valid value for IdServiceProviderEnumIdServiceProvider'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return IdServiceProviderEnumIdServiceProvider(v)
    else:
      return IdServiceProviderEnumIdServiceProvider(IdServiceProviderEnumIdServiceProvider._valid_values[v])

