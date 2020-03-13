# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class AuthzResourceEnumAuthzServiceResourceTypes(BaseType):
  _valid_values = [
    "UNKNOWN",
    "ALL",
  ]

  def __init__(self, val):
    if val not in AuthzResourceEnumAuthzServiceResourceTypes._valid_values:
      raise ValueError('{} is not a valid value for AuthzResourceEnumAuthzServiceResourceTypes'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return AuthzResourceEnumAuthzServiceResourceTypes(v)
    else:
      return AuthzResourceEnumAuthzServiceResourceTypes(AuthzResourceEnumAuthzServiceResourceTypes._valid_values[v])

