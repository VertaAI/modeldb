from .._protos.public.uac import Collaborator_pb2

from ._visibility import _Visibility


class Private(_Visibility):
    @property
    def _visibility(self):
        return Collaborator_pb2.ResourceVisibility.PRIVATE
