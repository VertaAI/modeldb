# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningGetRepositoryRequestResponse(BaseType):
  def __init__(self, repository=None):
    required = {
      "repository": False,
    }
    self.repository = repository

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningRepository import VersioningRepository


    tmp = d.get('repository', None)
    if tmp is not None:
      d['repository'] = VersioningRepository.from_json(tmp)

    return VersioningGetRepositoryRequestResponse(**d)
