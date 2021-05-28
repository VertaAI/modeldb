# pylint: disable=unidiomatic-typecheck

from verta._internal_utils import pagination_utils

class TestPaginationUtils:
    def test_from_proto(self):
        fn = pagination_utils.page_limit_from_proto
        assert fn(-1) is None
        assert fn(0) == 0
        assert fn(1) == 1

    def test_to_proto(self):
        fn = pagination_utils.page_limit_to_proto
        assert fn(None) == -1
        assert fn(-1) == -1
        assert fn(0) == 0
        assert fn(1) == 1
