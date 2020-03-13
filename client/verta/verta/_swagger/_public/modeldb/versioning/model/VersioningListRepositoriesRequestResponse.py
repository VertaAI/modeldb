# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningListRepositoriesRequestResponse(BaseType):
  def __init__(self, repository=None, total_records=None):
    required = {
      "repository": False,
      "total_records": False,
    }
    self.repository = repository
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .VersioningRepository import VersioningRepository

    

    tmp = d.get('repository', None)
    if tmp is not None:
      d['repository'] = [VersioningRepository.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return VersioningListRepositoriesRequestResponse(**d)
