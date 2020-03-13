# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ArtifactstoreStoreArtifact(BaseType):
  def __init__(self, key=None, path=None):
    required = {
      "key": False,
      "path": False,
    }
    self.key = key
    self.path = path

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('key', None)
    if tmp is not None:
      d['key'] = tmp
    tmp = d.get('path', None)
    if tmp is not None:
      d['path'] = tmp

    return ArtifactstoreStoreArtifact(**d)
