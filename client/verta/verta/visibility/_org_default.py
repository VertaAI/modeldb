from .._protos.public.uac import Collaborator_pb2

from ._visibility import _Visibility


class OrgDefault(_Visibility):
    def _to_public_within_org(self):
        return True

    @property
    def _visibility(self):
        return Collaborator_pb2.ResourceVisibility.ORG_DEFAULT
