# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class DatasetVisibilityEnumDatasetVisibility(BaseType):
  _valid_values = [
    "PRIVATE",
    "PUBLIC",
    "ORG_SCOPED_PUBLIC",
  ]

  def __init__(self, val):
    if val not in DatasetVisibilityEnumDatasetVisibility._valid_values:
      raise ValueError('{} is not a valid value for DatasetVisibilityEnumDatasetVisibility'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return DatasetVisibilityEnumDatasetVisibility(v)
    else:
      return DatasetVisibilityEnumDatasetVisibility(DatasetVisibilityEnumDatasetVisibility._valid_values[v])

