from .._protos.public.uac import Collaborator_pb2

from ._visibility import _Visibility


class Private(_Visibility):
    """
    Private and not visible to other organization members.

    Examples
    --------
    .. code-block:: python

        from verta.visibility import Private
        visibility = Private()
        client.create_project("My Project", workspace="my-org", visibility=visibility)

    """
    def _to_public_within_org(self):
        return False

    @property
    def _visibility(self):
        return Collaborator_pb2.ResourceVisibility.PRIVATE
