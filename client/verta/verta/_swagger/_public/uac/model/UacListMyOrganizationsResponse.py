# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacListMyOrganizationsResponse(BaseType):
  def __init__(self, organizations=None):
    required = {
      "organizations": False,
    }
    self.organizations = organizations

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacOrganization import UacOrganization


    tmp = d.get('organizations', None)
    if tmp is not None:
      d['organizations'] = [UacOrganization.from_json(tmp) for tmp in tmp]

    return UacListMyOrganizationsResponse(**d)
