from ._role import Role
from verta._protos.public.uac import GroupV2_pb2
from verta._protos.public.uac import RoleV2_pb2


class OrganizationV2:
    def __init__(self, conn, org_id):
        self.conn = conn
        self.org_id = org_id

    def get_groups(self):
        response_groups = self.conn.make_proto_request(
            "GET", f"/api/v2/uac-proxy/organization/{self.org_id}/groups"
        )
        return self.conn.maybe_proto_response(
            response_groups, GroupV2_pb2.SearchGroups.Response
        ).groups

    def get_roles(self):
        response_roles = self.conn.make_proto_request(
            "GET", f"/api/v2/uac-proxy/organization/{self.org_id}/roles"
        )
        return self.conn.maybe_proto_response(
            response_roles, RoleV2_pb2.SearchRolesV2.Response
        ).roles

    def create_role(self, name, resource_actions):
        return Role._create(self.conn, name, self.org_id, resource_actions).id
