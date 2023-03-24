# -*- coding: utf-8 -*-
from verta._protos.public.uac import RoleV2_pb2


class Role(object):
    """
    Object representing a Role.

    """

    def __init__(self, conn, msg):
        self.conn = conn
        self.msg = msg
        self.id = msg.id
        self.org_id = msg.org_id
        self.name = msg.name

    @classmethod
    def _create(cls, conn, name, org_id, resource_actions):
        Message = RoleV2_pb2.SetRoleV2
        msg = cls._create_msg(name, org_id, resource_actions)

        response = conn.make_proto_request(
            "POST",
            f"/api/v2/uac-proxy/organization/{org_id}/roles",
            body=Message(role=msg),
        )
        role = conn.must_proto_response(response, Message.Response).role

        print("created new Role : {}".format(role.name))
        return cls(conn, role)

    @classmethod
    def _create_msg(cls, name, org_id, resource_actions):
        Message = RoleV2_pb2.RoleV2
        msg = Message(name=name, org_id=org_id, resource_actions=resource_actions)
        return msg

    def delete(self):
        """
        Deletes this role.

        """
        Message = RoleV2_pb2.DeleteRoleV2
        endpoint = f"/api/v2/uac-proxy/organization/{self.org_id}/role/{self.id}"
        response = self.conn.make_proto_request("DELETE", endpoint)
        self.conn.must_proto_response(response, Message.Response)
