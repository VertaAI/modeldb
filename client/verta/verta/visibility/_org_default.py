from .._protos.public.uac import Collaborator_pb2

from ._visibility import _Visibility


class OrgDefault(_Visibility):
    """
    Organization-wide access with default permissions set by the org admin.

    Examples
    --------
    .. code-block:: python

        from verta.visibility import OrgDefault
        visibility = OrgDefault()
        client.create_project("My Project", workspace="my-org", visibility=visibility)

    """
    def _to_public_within_org(self):
        return True

    @property
    def _visibility(self):
        return Collaborator_pb2.ResourceVisibility.ORG_DEFAULT
