# -*- coding: utf-8 -*-
from verta._protos.public.uac import WorkspaceV2_pb2

class Workspace(object):
    """
    Object representing a Workspace.

    """

    def __init__(self, conn, msg):
        self.conn = conn
        self.msg = msg
        self.id = msg.id
        self.org_id = msg.org_id
        self.name = msg.name

    @classmethod
    def _create_proto(
        cls, conn, name, org_id, permissions
    ):
        Message = WorkspaceV2_pb2.SetWorkspaceV2
        msg = cls._create_msg(name, org_id, permissions)

        response = conn.make_proto_request(
            "POST",
            f"/api/v2/uac-proxy/organization/{org_id}/workspaces",
            body=Message(workspace=msg),
        )
        workspace = conn.must_proto_response(response, Message.Response).workspace

        print("created new Workspace : {}".format(workspace.name))
        return cls(conn, workspace)

    @classmethod
    def _create_msg(cls, name, org_id, permissions):
        Message = WorkspaceV2_pb2.WorkspaceV2
        msg = Message(name=name, org_id=org_id,
                      permissions = permissions)
        return msg

    def delete(self):
        """
        Deletes this workspace.

        """
        Message = WorkspaceV2_pb2.DeleteWorkspaceV2
        endpoint = "/api/v2/uac-proxy/organization/{}/workspace/{}".format(self.org_id, self.id)
        response = self.conn.make_proto_request("DELETE", endpoint)
        self.conn.must_proto_response(response, Message.Response)
