# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningS3DatasetDiff(BaseType):
  def __init__(self, path=None):
    required = {
      "path": False,
    }
    self.path = path

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningPathDatasetDiff import VersioningPathDatasetDiff


    tmp = d.get('path', None)
    if tmp is not None:
      d['path'] = VersioningPathDatasetDiff.from_json(tmp)

    return VersioningS3DatasetDiff(**d)
