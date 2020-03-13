# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningFolderElement(BaseType):
  def __init__(self, element_name=None, created_by_commit=None):
    required = {
      "element_name": False,
      "created_by_commit": False,
    }
    self.element_name = element_name
    self.created_by_commit = created_by_commit

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('element_name', None)
    if tmp is not None:
      d['element_name'] = tmp
    tmp = d.get('created_by_commit', None)
    if tmp is not None:
      d['created_by_commit'] = tmp

    return VersioningFolderElement(**d)
