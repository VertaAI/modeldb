# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModelResourceEnumModelDBServiceResourceTypes(BaseType):
  _valid_values = [
    "UNKNOWN",
    "ALL",
    "PROJECT",
    "EXPERIMENT",
    "EXPERIMENT_RUN",
    "DATASET",
    "DATASET_VERSION",
    "DASHBOARD",
  ]

  def __init__(self, val):
    if val not in ModelResourceEnumModelDBServiceResourceTypes._valid_values:
      raise ValueError('{} is not a valid value for ModelResourceEnumModelDBServiceResourceTypes'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return ModelResourceEnumModelDBServiceResourceTypes(v)
    else:
      return ModelResourceEnumModelDBServiceResourceTypes(ModelResourceEnumModelDBServiceResourceTypes._valid_values[v])

