# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbDatasetPartInfo(BaseType):
  def __init__(self, path=None, size=None, checksum=None, last_modified_at_source=None):
    required = {
      "path": False,
      "size": False,
      "checksum": False,
      "last_modified_at_source": False,
    }
    self.path = path
    self.size = size
    self.checksum = checksum
    self.last_modified_at_source = last_modified_at_source

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
    tmp = d.get('checksum', None)
    if tmp is not None:
      d['checksum'] = tmp
    tmp = d.get('last_modified_at_source', None)
    if tmp is not None:
      d['last_modified_at_source'] = tmp

    return ModeldbDatasetPartInfo(**d)
