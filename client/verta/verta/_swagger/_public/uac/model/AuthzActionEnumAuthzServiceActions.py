# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class AuthzActionEnumAuthzServiceActions(BaseType):
  _valid_values = [
    "UNKNOWN",
    "ALL",
    "IS_ALLOWED",
    "GET",
  ]

  def __init__(self, val):
    if val not in AuthzActionEnumAuthzServiceActions._valid_values:
      raise ValueError('{} is not a valid value for AuthzActionEnumAuthzServiceActions'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return AuthzActionEnumAuthzServiceActions(v)
    else:
      return AuthzActionEnumAuthzServiceActions(AuthzActionEnumAuthzServiceActions._valid_values[v])

