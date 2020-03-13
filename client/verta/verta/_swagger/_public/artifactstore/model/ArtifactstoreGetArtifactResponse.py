# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ArtifactstoreGetArtifactResponse(BaseType):
  def __init__(self, contents=None):
    required = {
      "contents": False,
    }
    self.contents = contents

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('contents', None)
    if tmp is not None:
      d['contents'] = tmp

    return ArtifactstoreGetArtifactResponse(**d)
