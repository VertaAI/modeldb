# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningDatasetBlob(BaseType):
  def __init__(self, s3=None, path=None):
    required = {
      "s3": False,
      "path": False,
    }
    self.s3 = s3
    self.path = path

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningS3DatasetBlob import VersioningS3DatasetBlob

    from .VersioningPathDatasetBlob import VersioningPathDatasetBlob


    tmp = d.get('s3', None)
    if tmp is not None:
      d['s3'] = VersioningS3DatasetBlob.from_json(tmp)
    tmp = d.get('path', None)
    if tmp is not None:
      d['path'] = VersioningPathDatasetBlob.from_json(tmp)

    return VersioningDatasetBlob(**d)
