import pytest

from google.protobuf import json_format

import verta.code


class TestGit:
    def test_no_autocapture(self):
        code_ver = verta.code.Git(_autocapture=False)

        # empty protobuf message
        assert not json_format.MessageToDict(code_ver._msg)


class TestNotebook:
    def test_no_autocapture(self):
        code_ver = verta.code.Notebook(_autocapture=False)

        # empty protobuf message
        assert not json_format.MessageToDict(code_ver._msg)
