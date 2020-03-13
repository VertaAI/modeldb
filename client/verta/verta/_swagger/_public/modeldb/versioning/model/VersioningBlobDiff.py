# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningBlobDiff(BaseType):
  def __init__(self, path=None, dataset=None):
    required = {
      "path": False,
      "dataset": False,
    }
    self.path = path
    self.dataset = dataset

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .VersioningDatasetDiff import VersioningDatasetDiff


    tmp = d.get('path', None)
    if tmp is not None:
      d['path'] = tmp
    tmp = d.get('dataset', None)
    if tmp is not None:
      d['dataset'] = VersioningDatasetDiff.from_json(tmp)

    return VersioningBlobDiff(**d)
