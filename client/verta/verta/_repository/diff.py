# -*- coding: utf-8 -*-

from __future__ import print_function

from .._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService


class Diff(object):
    def __init__(self, diffs):
        # TODO: order so that additions come after removal?
        self._diffs = diffs

    def __repr__(self):
        return '\n'.join(
            '\n'.join((
                "{} {}".format(
                    _VersioningService.DiffStatusEnum.DiffStatus.Name(diff.status),
                    '/'.join(diff.location),
                ),
                getattr(diff, diff.WhichOneof('content')).__repr__(),
            ))
            for diff
            in self._diffs
        )
