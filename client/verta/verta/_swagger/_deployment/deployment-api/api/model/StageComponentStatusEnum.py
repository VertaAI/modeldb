# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class StageComponentStatusEnum(BaseType):
  _valid_values = [
    "internal_error",
    "error",
    "running",
    "creating",
  ]

  def __init__(self, val):
    if val not in StageComponentStatusEnum._valid_values:
      raise ValueError('{} is not a valid value for StageComponentStatusEnum'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  @staticmethod
  def from_json(v):
    if isinstance(v, str):
      return StageComponentStatusEnum(v)
    else:
      return StageComponentStatusEnum(StageComponentStatusEnum._valid_values[v])

