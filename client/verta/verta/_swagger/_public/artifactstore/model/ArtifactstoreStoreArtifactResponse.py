# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ArtifactstoreStoreArtifactResponse(BaseType):
  def __init__(self, artifact_store_key=None, artifact_store_path=None):
    required = {
      "artifact_store_key": False,
      "artifact_store_path": False,
    }
    self.artifact_store_key = artifact_store_key
    self.artifact_store_path = artifact_store_path

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('artifact_store_key', None)
    if tmp is not None:
      d['artifact_store_key'] = tmp
    tmp = d.get('artifact_store_path', None)
    if tmp is not None:
      d['artifact_store_path'] = tmp

    return ArtifactstoreStoreArtifactResponse(**d)
