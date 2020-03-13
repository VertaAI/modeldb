# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningCreateCommitRequest(BaseType):
  def __init__(self, repository_id=None, commit=None, blobs=None):
    required = {
      "repository_id": False,
      "commit": False,
      "blobs": False,
    }
    self.repository_id = repository_id
    self.commit = commit
    self.blobs = blobs

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningRepositoryIdentification import VersioningRepositoryIdentification

    from .VersioningCommit import VersioningCommit

    from .VersioningBlobExpanded import VersioningBlobExpanded


    tmp = d.get('repository_id', None)
    if tmp is not None:
      d['repository_id'] = VersioningRepositoryIdentification.from_json(tmp)
    tmp = d.get('commit', None)
    if tmp is not None:
      d['commit'] = VersioningCommit.from_json(tmp)
    tmp = d.get('blobs', None)
    if tmp is not None:
      d['blobs'] = [VersioningBlobExpanded.from_json(tmp) for tmp in tmp]

    return VersioningCreateCommitRequest(**d)
