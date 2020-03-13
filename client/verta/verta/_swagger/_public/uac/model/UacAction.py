# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacAction(BaseType):
  def __init__(self, service=None, role_service_action=None, authz_service_action=None, modeldb_service_action=None):
    required = {
      "service": False,
      "role_service_action": False,
      "authz_service_action": False,
      "modeldb_service_action": False,
    }
    self.service = service
    self.role_service_action = role_service_action
    self.authz_service_action = authz_service_action
    self.modeldb_service_action = modeldb_service_action

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ServiceEnumService import ServiceEnumService

    from .RoleActionEnumRoleServiceActions import RoleActionEnumRoleServiceActions

    from .AuthzActionEnumAuthzServiceActions import AuthzActionEnumAuthzServiceActions

    from .ModelDBActionEnumModelDBServiceActions import ModelDBActionEnumModelDBServiceActions


    tmp = d.get('service', None)
    if tmp is not None:
      d['service'] = ServiceEnumService.from_json(tmp)
    tmp = d.get('role_service_action', None)
    if tmp is not None:
      d['role_service_action'] = RoleActionEnumRoleServiceActions.from_json(tmp)
    tmp = d.get('authz_service_action', None)
    if tmp is not None:
      d['authz_service_action'] = AuthzActionEnumAuthzServiceActions.from_json(tmp)
    tmp = d.get('modeldb_service_action', None)
    if tmp is not None:
      d['modeldb_service_action'] = ModelDBActionEnumModelDBServiceActions.from_json(tmp)

    return UacAction(**d)
