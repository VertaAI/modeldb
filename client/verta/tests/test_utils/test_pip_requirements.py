# pylint: disable=unidiomatic-typecheck

import subprocess
import sys

import pytest
import six
from verta._internal_utils import _pip_requirements_utils


class TestPipRequirementsUtils:
    def test_no_spacy_models_in_pip_freeze(self):
        spacy = pytest.importorskip("spacy")
        try:
            spacy.load("en_core_web_sm")
        except OSError:
            pytest.skip("SpaCy en_core_web_sm model not installed")

        # baseline: en_core_web_sm in pip freeze
        assert list(
            filter(
                _pip_requirements_utils.SPACY_MODEL_REGEX.match,
                six.ensure_str(
                    subprocess.check_output([sys.executable, "-m", "pip", "freeze"]),
                ).splitlines(),
            )
        )

        # en_core_web_sm not in our pip freeze util
        assert not list(
            filter(
                _pip_requirements_utils.SPACY_MODEL_REGEX.match,
                _pip_requirements_utils.get_pip_freeze(),
            )
        )
