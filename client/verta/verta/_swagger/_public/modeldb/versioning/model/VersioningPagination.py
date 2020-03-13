# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningPagination(BaseType):
  def __init__(self, page_number=None, page_limit=None):
    required = {
      "page_number": False,
      "page_limit": False,
    }
    self.page_number = page_number
    self.page_limit = page_limit

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('page_number', None)
    if tmp is not None:
      d['page_number'] = tmp
    tmp = d.get('page_limit', None)
    if tmp is not None:
      d['page_limit'] = tmp

    return VersioningPagination(**d)
