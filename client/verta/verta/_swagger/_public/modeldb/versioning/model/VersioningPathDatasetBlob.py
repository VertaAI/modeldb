# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningPathDatasetBlob(BaseType):
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
    from .VersioningPathDatasetComponentBlob import VersioningPathDatasetComponentBlob


    tmp = d.get('components', None)
    if tmp is not None:
      d['components'] = [VersioningPathDatasetComponentBlob.from_json(tmp) for tmp in tmp]

    return VersioningPathDatasetBlob(**d)
