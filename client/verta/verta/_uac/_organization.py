from ._role import Role
from verta._protos.public.uac import GroupV2_pb2 as _Group
from verta._protos.public.uac import RoleV2_pb2 as _Role
class OrganizationV2:
    def __init__(self, conn, org_id):
        self.conn = conn
        self.org_id = org_id

    def get_groups(self):
        response_groups = self.conn.make_proto_request(
            "GET", "/api/v1/uac-proxy/organization/{}/groups".format(self.org_id)
        )
        return self.conn.maybe_proto_response(response_groups, _Group.SearchGroups.Response).groups

    def get_roles(self):
        response_roles = self.conn.make_proto_request(
            "GET", "/api/v2/uac-proxy/organization/{}/roles".format(self.org_id)
        )
        return self.conn.maybe_proto_response(response_roles, _Role.SearchRolesV2.Response).roles

    def create_role(self, name, resource_actions):
        return Role._create_proto(self.conn, name, self.org_id, resource_actions).id
