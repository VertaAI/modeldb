# -*- coding: utf-8 -*-

import abc

from verta._vendored import six

from verta._protos.public.registry import StageService_pb2


@six.add_metaclass(abc.ABCMeta)
class _StageChange(object):
    """Base class for registry stage changes. Not for external use."""

    # StageService_pb2.StageEnum variant
    _STAGE = None

    def __init__(self, comment=None):
        if comment and not isinstance(comment, six.string_types):
            raise TypeError("`comment` must be type str, not {}".format(type(comment)))

        self._comment = comment

    def __repr__(self):
        lines = [type(self).__name__]

        if self._comment:
            lines.append("comment: {}".format(self._comment))

        return "\t\n".join(lines)

    @property
    def comment(self):
        return self._comment

    def _to_proto_request(self, model_version_id):
        return StageService_pb2.UpdateStageRequest(
            model_version_id=model_version_id,
            stage=self._STAGE,
            comment=self._comment,
        )
