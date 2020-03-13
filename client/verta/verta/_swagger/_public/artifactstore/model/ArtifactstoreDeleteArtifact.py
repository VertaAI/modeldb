# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ArtifactstoreDeleteArtifact(BaseType):
  def __init__(self, key=None):
    required = {
      "key": False,
    }
    self.key = key

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('key', None)
    if tmp is not None:
      d['key'] = tmp

    return ArtifactstoreDeleteArtifact(**d)
