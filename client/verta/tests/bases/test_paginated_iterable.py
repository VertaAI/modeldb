# -*- coding: utf-8 -*-

from verta._protos.public.modeldb import (
    ExperimentRunService_pb2 as _ExperimentRunService,
)
from verta._bases import _PaginatedIterable


class TestPaginatedIterable:
    class LL(_PaginatedIterable):
        def __init__(self, size):
            super(TestPaginatedIterable.LL, self).__init__(
                None, None, _ExperimentRunService.FindExperimentRuns()
            )
            self._size = size
            self._calls = 0

        def _call_back_end(self, msg):
            self._calls += 1

            start = (self._page_number(msg) - 1) * self._page_limit(msg)
            end = self._page_number(msg) * self._page_limit(msg)
            end = min(self._size, end)
            ids = list(range(start, end))
            objs = [_ExperimentRunService.ExperimentRun(id=str(i)) for i in ids]
            assert max(ids) <= self._size
            return objs, self._size

        def _create_element(self, el):
            return el

    def test_ll(self):
        size = 1000
        ll = TestPaginatedIterable.LL(size)

        for i, obj in enumerate(ll):
            assert str(i) == obj.id

    def test_limit(self):
        size = 1000
        limit = 10
        ll = TestPaginatedIterable.LL(size)
        ll.set_page_limit(limit)

        for i, obj in enumerate(ll):
            assert str(i) == obj.id

        assert ll._calls == size / limit
