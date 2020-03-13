# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningPathDatasetDiff(BaseType):
  def __init__(self, deleted=None, added=None, A=None, B=None):
    required = {
      "deleted": False,
      "added": False,
      "A": False,
      "B": False,
    }
    self.deleted = deleted
    self.added = added
    self.A = A
    self.B = B

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    from .VersioningPathDatasetBlob import VersioningPathDatasetBlob

    from .VersioningPathDatasetBlob import VersioningPathDatasetBlob


    tmp = d.get('deleted', None)
    if tmp is not None:
      d['deleted'] = tmp
    tmp = d.get('added', None)
    if tmp is not None:
      d['added'] = tmp
    tmp = d.get('A', None)
    if tmp is not None:
      d['A'] = VersioningPathDatasetBlob.from_json(tmp)
    tmp = d.get('B', None)
    if tmp is not None:
      d['B'] = VersioningPathDatasetBlob.from_json(tmp)

    return VersioningPathDatasetDiff(**d)
