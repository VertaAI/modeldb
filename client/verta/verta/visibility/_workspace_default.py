from .._protos.public.uac import Collaborator_pb2

from ._visibility import _Visibility


class _WorkspaceDefault(_Visibility):
    """
    Default for the workspace. Not for external use.

    """
    def _to_public_within_org(self):
        return None

    @property
    def _visibility(self):
        return Collaborator_pb2.ResourceVisibility.WORKSPACE_DEFAULT
