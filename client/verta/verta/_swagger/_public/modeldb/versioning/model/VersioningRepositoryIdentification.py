# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningRepositoryIdentification(BaseType):
  def __init__(self, named_id=None, repo_id=None):
    required = {
      "named_id": False,
      "repo_id": False,
    }
    self.named_id = named_id
    self.repo_id = repo_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningRepositoryNamedIdentification import VersioningRepositoryNamedIdentification

    

    tmp = d.get('named_id', None)
    if tmp is not None:
      d['named_id'] = VersioningRepositoryNamedIdentification.from_json(tmp)
    tmp = d.get('repo_id', None)
    if tmp is not None:
      d['repo_id'] = tmp

    return VersioningRepositoryIdentification(**d)
