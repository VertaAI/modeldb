# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGitSnapshot(BaseType):
  def __init__(self, filepaths=None, repo=None, hash=None, is_dirty=None):
    required = {
      "filepaths": False,
      "repo": False,
      "hash": False,
      "is_dirty": False,
    }
    self.filepaths = filepaths
    self.repo = repo
    self.hash = hash
    self.is_dirty = is_dirty

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    from .TernaryEnumTernary import TernaryEnumTernary


    tmp = d.get('filepaths', None)
    if tmp is not None:
      d['filepaths'] = [tmp for tmp in tmp]
    tmp = d.get('repo', None)
    if tmp is not None:
      d['repo'] = tmp
    tmp = d.get('hash', None)
    if tmp is not None:
      d['hash'] = tmp
    tmp = d.get('is_dirty', None)
    if tmp is not None:
      d['is_dirty'] = TernaryEnumTernary.from_json(tmp)

    return ModeldbGitSnapshot(**d)
