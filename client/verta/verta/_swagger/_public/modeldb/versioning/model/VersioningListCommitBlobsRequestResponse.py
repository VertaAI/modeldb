# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningListCommitBlobsRequestResponse(BaseType):
  def __init__(self, blobs=None, total_records=None):
    required = {
      "blobs": False,
      "total_records": False,
    }
    self.blobs = blobs
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningBlobExpanded import VersioningBlobExpanded

    

    tmp = d.get('blobs', None)
    if tmp is not None:
      d['blobs'] = [VersioningBlobExpanded.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return VersioningListCommitBlobsRequestResponse(**d)
