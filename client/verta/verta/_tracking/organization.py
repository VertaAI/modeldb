# -*- coding: utf-8 -*-
from .._protos.public.uac import Organization_pb2 as _Organization
from .._protos.public.common import CommonService_pb2 as _CommonCommonService


class CollaboratorType:
    def __init__(self, global_collaborator_type=None, default_repo_collaborator_type=None,
                 default_endpoint_collaborator_type=None, default_dataset_collaborator_type=None):
        self.global_collaborator_type = global_collaborator_type
        self.default_repo_collaborator_type = default_repo_collaborator_type
        self.default_endpoint_collaborator_type = default_endpoint_collaborator_type
        self.default_dataset_collaborator_type = default_dataset_collaborator_type


class Organization:
    """
    Object representing an Organization.

    """

    def __init__(self, conn, msg):
        self.conn = conn
        self.msg = msg
        self.id = msg.id
        self.name = msg.name

    @classmethod
    def _create(cls, conn, name, desc=None, collaborator_type=None, global_can_deploy=False):
        Message = _Organization.SetOrganization
        msg = cls._create_msg(name, desc, collaborator_type, global_can_deploy)

        response = conn.make_proto_request("POST",
                                           "/api/v1/uac-proxy/organization/setOrganization",
                                           body=Message(organization=msg))
        org = conn.must_proto_response(response, Message.Response).organization

        print("created new Organization: {}".format(org.name))
        return cls(conn, org)

    @classmethod
    def _create_msg(cls, name, desc, collaborator_type, global_can_deploy):
        Message = _Organization.Organization
        if not collaborator_type:
            collaborator_type = CollaboratorType()
        if global_can_deploy:
            can_deploy_value = _CommonCommonService.TernaryEnum.Ternary.TRUE
        else:
            can_deploy_value = _CommonCommonService.TernaryEnum.Ternary.FALSE
        msg = Message(name=name, description=desc, global_can_deploy=can_deploy_value)
        for key in collaborator_type.__dict__:
            try:
                attr = getattr(collaborator_type, key)
                if not attr:
                    value = _CommonCommonService.CollaboratorTypeEnum.CollaboratorType.READ_ONLY
                else:
                    value = _CommonCommonService.CollaboratorTypeEnum.CollaboratorType.Value(attr)
                setattr(msg, key, value)
            except ValueError:
                unknown_value_error = "Unknown value specified for {}. Possible values are READ_ONLY, READ_WRITE."
                raise ValueError(unknown_value_error.format(key))
        return msg

    @classmethod
    def _get_by_name(cls, conn, name):
        Message = _Organization.GetOrganizationByName
        msg = Message(org_name=name)

        response = conn.make_proto_request("GET",
                                           "/api/v1/uac-proxy/organization/getOrganizationByName",
                                           params=msg)
        org = conn.must_proto_response(response, Message.Response).organization
        return cls(conn, org)

    """
    Adds member to an organization

    Parameters
    ----------
    share_with : str
        Represents email or username.

    """

    def add_member(self, share_with):
        Message = _Organization.AddUser

        response = self.conn.make_proto_request("POST",
                                           "/api/v1/uac-proxy/organization/addUser",
                                           body=Message(org_id=self.id, share_with=share_with))
        status = self.conn.must_proto_response(response, Message.Response).status

    def delete(self):
        """
        Deletes this organization.

        """
        Message = _Organization.DeleteOrganization
        endpoint = "/api/v1/uac-proxy/organization/deleteOrganization"
        msg = Message(org_id=self.id)
        response = self.conn.make_proto_request("POST", endpoint, body=msg)
        self.conn.must_proto_response(response, Message.Response)
