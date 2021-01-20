import abc

import six

from .._protos.public.uac import Collaborator_pb2


@six.add_metaclass(abc.ABCMeta)
class _Visibility(object):
    @property
    def custom_permission(self):
        return Collaborator_pb2.CollaboratorPermissions()

    @abc.abstractproperty
    def visibility(self):
        raise NotImplementedError
