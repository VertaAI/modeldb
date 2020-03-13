# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class DatasetTypeEnumDatasetType(BaseType):
  _valid_values = [
    "RAW",
    "PATH",
    "QUERY",
  ]

  def __init__(self, val):
    if val not in DatasetTypeEnumDatasetType._valid_values:
      raise ValueError('{} is not a valid value for DatasetTypeEnumDatasetType'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return DatasetTypeEnumDatasetType(v)
    else:
      return DatasetTypeEnumDatasetType(DatasetTypeEnumDatasetType._valid_values[v])

