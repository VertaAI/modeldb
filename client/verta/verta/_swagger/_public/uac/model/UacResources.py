# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacResources(BaseType):
  def __init__(self, service=None, resource_ids=None, role_service_resource_type=None, authz_service_resource_type=None, modeldb_service_resource_type=None):
    required = {
      "service": False,
      "resource_ids": False,
      "role_service_resource_type": False,
      "authz_service_resource_type": False,
      "modeldb_service_resource_type": False,
    }
    self.service = service
    self.resource_ids = resource_ids
    self.role_service_resource_type = role_service_resource_type
    self.authz_service_resource_type = authz_service_resource_type
    self.modeldb_service_resource_type = modeldb_service_resource_type

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ServiceEnumService import ServiceEnumService

    
    from .RoleResourceEnumRoleServiceResourceTypes import RoleResourceEnumRoleServiceResourceTypes

    from .AuthzResourceEnumAuthzServiceResourceTypes import AuthzResourceEnumAuthzServiceResourceTypes

    from .ModelResourceEnumModelDBServiceResourceTypes import ModelResourceEnumModelDBServiceResourceTypes


    tmp = d.get('service', None)
    if tmp is not None:
      d['service'] = ServiceEnumService.from_json(tmp)
    tmp = d.get('resource_ids', None)
    if tmp is not None:
      d['resource_ids'] = [tmp for tmp in tmp]
    tmp = d.get('role_service_resource_type', None)
    if tmp is not None:
      d['role_service_resource_type'] = RoleResourceEnumRoleServiceResourceTypes.from_json(tmp)
    tmp = d.get('authz_service_resource_type', None)
    if tmp is not None:
      d['authz_service_resource_type'] = AuthzResourceEnumAuthzServiceResourceTypes.from_json(tmp)
    tmp = d.get('modeldb_service_resource_type', None)
    if tmp is not None:
      d['modeldb_service_resource_type'] = ModelResourceEnumModelDBServiceResourceTypes.from_json(tmp)

    return UacResources(**d)
