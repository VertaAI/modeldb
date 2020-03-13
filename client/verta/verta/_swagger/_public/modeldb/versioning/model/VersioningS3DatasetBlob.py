# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningS3DatasetBlob(BaseType):
  def __init__(self, components=None):
    required = {
      "components": False,
    }
    self.components = components

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningS3DatasetComponentBlob import VersioningS3DatasetComponentBlob


    tmp = d.get('components', None)
    if tmp is not None:
      d['components'] = [VersioningS3DatasetComponentBlob.from_json(tmp) for tmp in tmp]

    return VersioningS3DatasetBlob(**d)
