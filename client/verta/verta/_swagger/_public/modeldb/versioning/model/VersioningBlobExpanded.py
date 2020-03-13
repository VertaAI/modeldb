# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningBlobExpanded(BaseType):
  def __init__(self, path=None, blob=None):
    required = {
      "path": False,
      "blob": False,
    }
    self.path = path
    self.blob = blob

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .VersioningBlob import VersioningBlob


    tmp = d.get('path', None)
    if tmp is not None:
      d['path'] = tmp
    tmp = d.get('blob', None)
    if tmp is not None:
      d['blob'] = VersioningBlob.from_json(tmp)

    return VersioningBlobExpanded(**d)
