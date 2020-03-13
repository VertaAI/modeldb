# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningGetCommitFolderRequestResponse(BaseType):
  def __init__(self, folder=None):
    required = {
      "folder": False,
    }
    self.folder = folder

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningFolder import VersioningFolder


    tmp = d.get('folder', None)
    if tmp is not None:
      d['folder'] = VersioningFolder.from_json(tmp)

    return VersioningGetCommitFolderRequestResponse(**d)
