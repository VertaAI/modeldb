# -*- coding: utf-8 -*-

import sys

from verta.environment import Python


class TestPython:
    def test_python_version(self):
        """Verify ``python_version`` property matches ``sys.version_info``."""
        python_version = Python([]).python_version

        # tuple values match
        assert tuple(python_version) == tuple(sys.version_info[:3])

        # namedtuple fields match
        for field in ["major", "minor", "micro"]:
            assert getattr(python_version, field) == getattr(sys.version_info, field)
