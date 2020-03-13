# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ArtifactstoreStoreArtifactWithStreamResponse(BaseType):
  def __init__(self, cloud_file_key=None, cloud_file_path=None):
    required = {
      "cloud_file_key": False,
      "cloud_file_path": False,
    }
    self.cloud_file_key = cloud_file_key
    self.cloud_file_path = cloud_file_path

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('cloud_file_key', None)
    if tmp is not None:
      d['cloud_file_key'] = tmp
    tmp = d.get('cloud_file_path', None)
    if tmp is not None:
      d['cloud_file_path'] = tmp

    return ArtifactstoreStoreArtifactWithStreamResponse(**d)
