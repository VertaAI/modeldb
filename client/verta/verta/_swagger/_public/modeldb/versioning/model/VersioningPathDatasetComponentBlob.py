# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningPathDatasetComponentBlob(BaseType):
  def __init__(self, path=None, size=None, last_modified_at_source=None, sha256=None, md5=None):
    required = {
      "path": False,
      "size": False,
      "last_modified_at_source": False,
      "sha256": False,
      "md5": False,
    }
    self.path = path
    self.size = size
    self.last_modified_at_source = last_modified_at_source
    self.sha256 = sha256
    self.md5 = md5

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    

    tmp = d.get('path', None)
    if tmp is not None:
      d['path'] = tmp
    tmp = d.get('size', None)
    if tmp is not None:
      d['size'] = tmp
    tmp = d.get('last_modified_at_source', None)
    if tmp is not None:
      d['last_modified_at_source'] = tmp
    tmp = d.get('sha256', None)
    if tmp is not None:
      d['sha256'] = tmp
    tmp = d.get('md5', None)
    if tmp is not None:
      d['md5'] = tmp

    return VersioningPathDatasetComponentBlob(**d)
