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

    @classmethod
    def _create(cls, conn, name, desc=None, collaborator_type=None, global_can_deploy=False):
        Message = _Organization.SetOrganization
        msg = cls._create_msg(name, desc, collaborator_type, global_can_deploy)

        response = conn.make_proto_request("POST",
                                           "/api/v1/uac-proxy/organization/setOrganization",
                                           body=Message(organization=msg))
        org = conn.must_proto_response(response, Message.Response).organization

        print("created new Organization: {}".format(org.name))
        return org

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
