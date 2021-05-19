# -*- coding: utf-8 -*-

from __future__ import print_function

from .._protos.public.modeldb.versioning import Enums_pb2 as _VersioningEnumsProtos


class Diff(object):
    r"""A diff between :class:`~verta.repository.Commit`\ s.

    There should not be a need to instantiate this class directly; please use
    :meth:`Commit.diff_from() <verta.repository.Commit.diff_from>`.

    Examples
    --------
    .. code-block:: python

        diff = commit_b.diff_from(commit_a)
        print(diff)

    """

    def __init__(self, diffs):
        # TODO: order so that additions come after removal?
        self._diffs = diffs

    def __repr__(self):
        return '\n'.join(
            '\n'.join((
                "{} {}".format(
                    _VersioningEnumsProtos.DiffStatusEnum.DiffStatus.Name(diff.status),
                    '/'.join(diff.location),
                ),
                getattr(diff, diff.WhichOneof('content')).__repr__(),
            ))
            for diff
            in self._diffs
        )
