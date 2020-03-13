# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningCommit(BaseType):
  def __init__(self, parent_shas=None, message=None, date_created=None, author=None):
    required = {
      "parent_shas": False,
      "message": False,
      "date_created": False,
      "author": False,
    }
    self.parent_shas = parent_shas
    self.message = message
    self.date_created = date_created
    self.author = author

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    

    tmp = d.get('parent_shas', None)
    if tmp is not None:
      d['parent_shas'] = [tmp for tmp in tmp]
    tmp = d.get('message', None)
    if tmp is not None:
      d['message'] = tmp
    tmp = d.get('date_created', None)
    if tmp is not None:
      d['date_created'] = tmp
    tmp = d.get('author', None)
    if tmp is not None:
      d['author'] = tmp

    return VersioningCommit(**d)
