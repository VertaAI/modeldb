from .._protos.public.uac import Collaborator_pb2

from ._resource_visibility import _Visibility


class _WorkspaceDefault(_Visibility):
    @property
    def visibility(self):
        return Collaborator_pb2.ResourceVisibility.WORKSPACE_DEFAULT
