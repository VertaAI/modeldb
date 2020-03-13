# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacSetOrganizationResponse(BaseType):
  def __init__(self, organization=None):
    required = {
      "organization": False,
    }
    self.organization = organization

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .UacOrganization import UacOrganization


    tmp = d.get('organization', None)
    if tmp is not None:
      d['organization'] = UacOrganization.from_json(tmp)

    return UacSetOrganizationResponse(**d)
