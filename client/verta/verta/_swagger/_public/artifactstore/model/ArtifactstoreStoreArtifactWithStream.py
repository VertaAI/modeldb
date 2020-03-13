# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ArtifactstoreStoreArtifactWithStream(BaseType):
  def __init__(self, key=None, client_file=None):
    required = {
      "key": False,
      "client_file": False,
    }
    self.key = key
    self.client_file = client_file

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('key', None)
    if tmp is not None:
      d['key'] = tmp
    tmp = d.get('client_file', None)
    if tmp is not None:
      d['client_file'] = tmp

    return ArtifactstoreStoreArtifactWithStream(**d)
