# -*- coding: utf-8 -*-

import hypothesis
import hypothesis.strategies as st

from verta._internal_utils import arg_handler


class TestPrependSlash:
    @hypothesis.given(path=st.text(min_size=1))
    def test_with_slash(self, path):
        path = "/" + path

        assert arg_handler.ensure_starts_with_slash(path) == path

    @hypothesis.given(path=st.text(min_size=1))
    def test_without_slash(self, path):
        hypothesis.assume(not path.startswith("/"))

        expected_path = "/" + path
        assert arg_handler.ensure_starts_with_slash(path) == expected_path
