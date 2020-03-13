# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningGetCommitRequestResponse(BaseType):
  def __init__(self, commit=None):
    required = {
      "commit": False,
    }
    self.commit = commit

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningCommit import VersioningCommit


    tmp = d.get('commit', None)
    if tmp is not None:
      d['commit'] = VersioningCommit.from_json(tmp)

    return VersioningGetCommitRequestResponse(**d)
