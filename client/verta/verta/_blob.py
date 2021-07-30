# -*- coding: utf-8 -*-

from __future__ import print_function

import abc

from verta.external import six


@six.add_metaclass(abc.ABCMeta)
class Blob(object):
    def __eq__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        return self._as_proto() == other._as_proto()

    def __ne__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        return not self.__eq__(other)

    @classmethod
    @abc.abstractmethod
    def _from_proto(cls, blob_msg):
        """
        Returns a blob from `blob_msg`.

        Parameters
        ----------
        blob_msg : _VersioningService.Blob

        Returns
        -------
        blob : class instance

        """
        pass

    @abc.abstractmethod
    def _as_proto(self):
        """
        Returns this blob as a protobuf message.

        Returns
        -------
        blob_msg : _VersioningService.Blob

        """
        pass

    @staticmethod
    def blob_msg_to_object(blob_msg):
        """Deserialize a blob protobuf message into an instance.

        Parameters
        ----------
        blob_msg : :class:`VersioningService_pb2.Blob`

        Returns
        -------
        instance of subclass of :class:`Blob`

        """
        from verta import code, configuration, dataset, environment

        # TODO: make this more concise
        content_type = blob_msg.WhichOneof('content')
        content_subtype = None
        blob_cls = None
        if content_type == 'code':
            content_subtype = blob_msg.code.WhichOneof('content')
            if content_subtype == 'git':
                blob_cls = code.Git
            elif content_subtype == 'notebook':
                blob_cls = code.Notebook
        elif content_type == 'config':
            blob_cls = configuration.Hyperparameters
        elif content_type == 'dataset':
            content_subtype = blob_msg.dataset.WhichOneof('content')
            if content_subtype == 's3':
                blob_cls = dataset.S3
            elif content_subtype == 'path':
                blob_cls = dataset.Path
        elif content_type == 'environment':
            content_subtype = blob_msg.environment.WhichOneof('content')
            if content_subtype == 'python':
                blob_cls = environment.Python
            elif content_subtype == 'docker':
                raise NotImplementedError

        if blob_cls is None:
            if content_subtype is None:
                raise NotImplementedError("found unexpected content type {};"
                                        " please notify the Verta development team".format(content_type))
            else:
                raise NotImplementedError("found unexpected {} type {};"
                                        " please notify the Verta development team".format(content_type, content_subtype))

        return blob_cls._from_proto(blob_msg)
