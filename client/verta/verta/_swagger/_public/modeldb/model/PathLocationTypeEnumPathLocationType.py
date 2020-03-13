# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class PathLocationTypeEnumPathLocationType(BaseType):
  _valid_values = [
    "LOCAL_FILE_SYSTEM",
    "NETWORK_FILE_SYSTEM",
    "HADOOP_FILE_SYSTEM",
    "S3_FILE_SYSTEM",
  ]

  def __init__(self, val):
    if val not in PathLocationTypeEnumPathLocationType._valid_values:
      raise ValueError('{} is not a valid value for PathLocationTypeEnumPathLocationType'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return PathLocationTypeEnumPathLocationType(v)
    else:
      return PathLocationTypeEnumPathLocationType(PathLocationTypeEnumPathLocationType._valid_values[v])

