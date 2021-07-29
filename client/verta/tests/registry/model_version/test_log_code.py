# -*- coding: utf-8 -*-

import pytest

from verta.code import Git, Notebook


pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestLogCode:
    def test_log_code(self, model_version):
        key1, key2, key3 = "version1", "version2", "version3"
        version1 = Git(
            repo_url="git@github.com:VertaAI/models.git",
            commit_hash="52f3d22",
            autocapture=False,
        )
        version2 = Git(
            repo_url="git@github.com:VertaAI/data-processing.git",
            commit_hash="26f9787",
            autocapture=False,
        )
        version3 = Notebook(
            "Spam-Detection.ipynb",
            _autocapture=False,
        )

        model_version.log_code(key1, version1)
        model_version.log_code(key2, version2)
        model_version.log_code(key3, version3)

        assert model_version.get_code(key1) == version1
        assert model_version.get_code(key2) == version2
        assert model_version.get_code(key3) == version3
