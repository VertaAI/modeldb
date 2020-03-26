import pytest

from google.protobuf import json_format

import verta.code


class TestGit:
    def test_no_autocapture(self):
        code_ver = verta.code.Git(_autocapture=False)

        # protobuf message is empty
        assert not json_format.MessageToDict(
            code_ver._msg,
            including_default_value_fields=False,
        )


class TestNotebook:
    def test_no_autocapture(self):
        code_ver = verta.code.Notebook(_autocapture=False)

        # protobuf message is empty
        assert not json_format.MessageToDict(
            code_ver._msg,
            including_default_value_fields=False,
        )
