# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbArtifact(BaseType):
  def __init__(self, key=None, path=None, path_only=None, artifact_type=None, linked_artifact_id=None, filename_extension=None):
    required = {
      "key": False,
      "path": False,
      "path_only": False,
      "artifact_type": False,
      "linked_artifact_id": False,
      "filename_extension": False,
    }
    self.key = key
    self.path = path
    self.path_only = path_only
    self.artifact_type = artifact_type
    self.linked_artifact_id = linked_artifact_id
    self.filename_extension = filename_extension

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    from .ArtifactTypeEnumArtifactType import ArtifactTypeEnumArtifactType

    
    

    tmp = d.get('key', None)
    if tmp is not None:
      d['key'] = tmp
    tmp = d.get('path', None)
    if tmp is not None:
      d['path'] = tmp
    tmp = d.get('path_only', None)
    if tmp is not None:
      d['path_only'] = tmp
    tmp = d.get('artifact_type', None)
    if tmp is not None:
      d['artifact_type'] = ArtifactTypeEnumArtifactType.from_json(tmp)
    tmp = d.get('linked_artifact_id', None)
    if tmp is not None:
      d['linked_artifact_id'] = tmp
    tmp = d.get('filename_extension', None)
    if tmp is not None:
      d['filename_extension'] = tmp

    return ModeldbArtifact(**d)
