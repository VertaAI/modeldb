# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningFolder(BaseType):
  def __init__(self, blobs=None, sub_folders=None):
    required = {
      "blobs": False,
      "sub_folders": False,
    }
    self.blobs = blobs
    self.sub_folders = sub_folders

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningFolderElement import VersioningFolderElement

    from .VersioningFolderElement import VersioningFolderElement


    tmp = d.get('blobs', None)
    if tmp is not None:
      d['blobs'] = [VersioningFolderElement.from_json(tmp) for tmp in tmp]
    tmp = d.get('sub_folders', None)
    if tmp is not None:
      d['sub_folders'] = [VersioningFolderElement.from_json(tmp) for tmp in tmp]

    return VersioningFolder(**d)
