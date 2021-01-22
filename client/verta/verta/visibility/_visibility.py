import abc

from ..external import six

from .._protos.public.uac import Collaborator_pb2


@six.add_metaclass(abc.ABCMeta)
class _Visibility(object):
    @property
    def _custom_permission(self):
        return Collaborator_pb2.CollaboratorPermissions()

    @abc.abstractproperty
    def _visibility(self):
        raise NotImplementedError
