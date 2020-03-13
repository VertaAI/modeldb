# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class JobTypeEnumJobType(BaseType):
  _valid_values = [
    "KUBERNETES_JOB",
  ]

  def __init__(self, val):
    if val not in JobTypeEnumJobType._valid_values:
      raise ValueError('{} is not a valid value for JobTypeEnumJobType'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return JobTypeEnumJobType(v)
    else:
      return JobTypeEnumJobType(JobTypeEnumJobType._valid_values[v])

