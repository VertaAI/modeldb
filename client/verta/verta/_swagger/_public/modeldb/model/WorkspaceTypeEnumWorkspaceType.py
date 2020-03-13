# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class WorkspaceTypeEnumWorkspaceType(BaseType):
  _valid_values = [
    "UNKNOWN",
    "ORGANIZATION",
    "USER",
  ]

  def __init__(self, val):
    if val not in WorkspaceTypeEnumWorkspaceType._valid_values:
      raise ValueError('{} is not a valid value for WorkspaceTypeEnumWorkspaceType'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return WorkspaceTypeEnumWorkspaceType(v)
    else:
      return WorkspaceTypeEnumWorkspaceType(WorkspaceTypeEnumWorkspaceType._valid_values[v])

