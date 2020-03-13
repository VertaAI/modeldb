# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class RoleActionEnumRoleServiceActions(BaseType):
  _valid_values = [
    "UNKNOWN",
    "ALL",
    "GET_BY_ID",
    "GET_BY_NAME",
    "CREATE",
    "UPDATE",
    "LIST",
    "DELETE",
  ]

  def __init__(self, val):
    if val not in RoleActionEnumRoleServiceActions._valid_values:
      raise ValueError('{} is not a valid value for RoleActionEnumRoleServiceActions'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return RoleActionEnumRoleServiceActions(v)
    else:
      return RoleActionEnumRoleServiceActions(RoleActionEnumRoleServiceActions._valid_values[v])

