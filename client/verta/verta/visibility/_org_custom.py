from .._protos.public.uac import Collaborator_pb2
from .._protos.public.common import CommonService_pb2

from ._visibility import _Visibility


class OrgCustom(_Visibility):
    def __init__(self, write=False, deploy=False):
        if not isinstance(write, bool):
            raise TypeError("`write` must be of type bool, not {}".format(type(write)))
        if not isinstance(deploy, bool):
            raise TypeError("`deploy` must be of type bool, not {}".format(type(deploy)))

        self._write = write
        self._deploy = deploy

    def __repr__(self):
        return "<{}(write={}, deploy={}) visibility>".format(self.__class__.__name__, self._write, self._deploy)

    def _to_public_within_org(self):
        # NOTE: old backends will unavoidably not receive `_write` and `_deploy`
        return True

    @property
    def _custom_permission(self):
        if self._write:
            collaborator_type = CommonService_pb2.CollaboratorTypeEnum.READ_WRITE
        else:
            collaborator_type = CommonService_pb2.CollaboratorTypeEnum.READ_ONLY

        if self._deploy:
            can_deploy = CommonService_pb2.TernaryEnum.TRUE
        else:
            can_deploy = CommonService_pb2.TernaryEnum.FALSE

        return Collaborator_pb2.CollaboratorPermissions(
            collaborator_type=collaborator_type,
            can_deploy=can_deploy,
        )

    @property
    def _visibility(self):
        return Collaborator_pb2.ResourceVisibility.ORG_CUSTOM
