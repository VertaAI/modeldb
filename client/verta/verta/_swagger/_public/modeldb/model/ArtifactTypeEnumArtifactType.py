# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ArtifactTypeEnumArtifactType(BaseType):
  _valid_values = [
    "IMAGE",
    "MODEL",
    "TENSORBOARD",
    "DATA",
    "BLOB",
    "STRING",
    "CODE",
  ]

  def __init__(self, val):
    if val not in ArtifactTypeEnumArtifactType._valid_values:
      raise ValueError('{} is not a valid value for ArtifactTypeEnumArtifactType'.format(val))
    self.value = val

  def to_json(self):
    return self.value

  def from_json(v):
    if isinstance(v, str):
      return ArtifactTypeEnumArtifactType(v)
    else:
      return ArtifactTypeEnumArtifactType(ArtifactTypeEnumArtifactType._valid_values[v])

