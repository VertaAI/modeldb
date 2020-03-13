# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbRawDatasetVersionInfo(BaseType):
  def __init__(self, size=None, features=None, num_records=None, object_path=None, checksum=None):
    required = {
      "size": False,
      "features": False,
      "num_records": False,
      "object_path": False,
      "checksum": False,
    }
    self.size = size
    self.features = features
    self.num_records = num_records
    self.object_path = object_path
    self.checksum = checksum

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    

    tmp = d.get('size', None)
    if tmp is not None:
      d['size'] = tmp
    tmp = d.get('features', None)
    if tmp is not None:
      d['features'] = [tmp for tmp in tmp]
    tmp = d.get('num_records', None)
    if tmp is not None:
      d['num_records'] = tmp
    tmp = d.get('object_path', None)
    if tmp is not None:
      d['object_path'] = tmp
    tmp = d.get('checksum', None)
    if tmp is not None:
      d['checksum'] = tmp

    return ModeldbRawDatasetVersionInfo(**d)
