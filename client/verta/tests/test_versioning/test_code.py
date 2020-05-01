import pytest

from google.protobuf import json_format

import verta.code
from verta._internal_utils import _git_utils


class TestGit:
    def test_no_autocapture(self):
        code_ver = verta.code.Git(_autocapture=False)

        # protobuf message is empty
        assert not json_format.MessageToDict(
            code_ver._msg,
            including_default_value_fields=False,
        )

    def test_repr(self):
        """Tests that __repr__() executes without error"""
        try:
            _git_utils.get_git_repo_root_dir()
        except OSError:
            pytest.skip("not in git repo")

        code_ver = verta.code.Git()

        assert code_ver.__repr__()


class TestNotebook:
    def test_no_autocapture(self):
        code_ver = verta.code.Notebook(_autocapture=False)

        # protobuf message is empty
        assert not json_format.MessageToDict(
            code_ver._msg,
            including_default_value_fields=False,
        )
