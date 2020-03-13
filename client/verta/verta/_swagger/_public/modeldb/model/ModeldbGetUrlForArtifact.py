# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetUrlForArtifact(BaseType):
  def __init__(self, id=None, key=None, method=None, artifact_type=None):
    required = {
      "id": False,
      "key": False,
      "method": False,
      "artifact_type": False,
    }
    self.id = id
    self.key = key
    self.method = method
    self.artifact_type = artifact_type

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    from .ArtifactTypeEnumArtifactType import ArtifactTypeEnumArtifactType


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('key', None)
    if tmp is not None:
      d['key'] = tmp
    tmp = d.get('method', None)
    if tmp is not None:
      d['method'] = tmp
    tmp = d.get('artifact_type', None)
    if tmp is not None:
      d['artifact_type'] = ArtifactTypeEnumArtifactType.from_json(tmp)

    return ModeldbGetUrlForArtifact(**d)
