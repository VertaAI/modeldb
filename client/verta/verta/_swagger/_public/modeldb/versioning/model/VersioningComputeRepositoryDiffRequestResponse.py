# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningComputeRepositoryDiffRequestResponse(BaseType):
  def __init__(self, diffs=None):
    required = {
      "diffs": False,
    }
    self.diffs = diffs

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningBlobDiff import VersioningBlobDiff


    tmp = d.get('diffs', None)
    if tmp is not None:
      d['diffs'] = [VersioningBlobDiff.from_json(tmp) for tmp in tmp]

    return VersioningComputeRepositoryDiffRequestResponse(**d)
