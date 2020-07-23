# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class StageStatusEnum(BaseType):
  _valid_values = [
    "inactive",
    "active",
    "updating",
    "creating",
    "error",
  ]

  def __init__(self, val):
    if val not in StageStatusEnum._valid_values:
      raise ValueError('{} is not a valid value for StageStatusEnum'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  @staticmethod
  def from_json(v):
    if isinstance(v, str):
      return StageStatusEnum(v)
    else:
      return StageStatusEnum(StageStatusEnum._valid_values[v])

