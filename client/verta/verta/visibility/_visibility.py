import abc

from ..external import six

from .._protos.public.uac import Collaborator_pb2
from .._protos.public.common import CommonService_pb2

from .._internal_utils import _utils


@six.add_metaclass(abc.ABCMeta)
class _Visibility(object):
    """
    Base class for visibility. Not for external use.

    """
    def __repr__(self):
        return "<{} visibility>".format(self.__class__.__name__)

    @staticmethod
    def _from_public_within_org(public_within_org):
        # imports here to avoid circular import in Python 2
        from . import (
            OrgDefault,
            Private,
            _WorkspaceDefault,
        )

        if public_within_org is None:
            return _WorkspaceDefault()
        elif public_within_org:
            return OrgDefault()
        else:
            return Private()

    @abc.abstractmethod
    def _to_public_within_org(self):
        raise NotImplementedError

    @staticmethod
    def _translate_public_within_org(visibility, public_within_org):
        """
        Helper that combines _from_public_within_org() and _to_public_within_org().

        Enables reuse in Repository and Endpoint, which still need to be
        refactored to subclass `_ModelDBEntiy`.

        """
        # imports here to avoid circular import in Python 2
        from . import (
            _WorkspaceDefault,
        )

        if visibility is None and public_within_org is None:
            visibility = _WorkspaceDefault()
        elif visibility is None:
            visibility = _Visibility._from_public_within_org(public_within_org)
        elif public_within_org is None:
            public_within_org = visibility._to_public_within_org()
        else:
            raise ValueError("cannot set both `visibility` and `public_within_org`")

        return visibility, public_within_org

    @property
    def _custom_permission(self):
        return Collaborator_pb2.CollaboratorPermissions()

    @abc.abstractproperty
    def _visibility(self):
        raise NotImplementedError

    @property
    def _collaborator_type_str(self):
        """Mainly for endpoint."""
        return CommonService_pb2.CollaboratorTypeEnum.CollaboratorType.Name(
            self._custom_permission.collaborator_type
        )

    @property
    def _visibility_str(self):
        """Mainly for endpoint."""
        return Collaborator_pb2.ResourceVisibility.Name(
            self._visibility
        )
