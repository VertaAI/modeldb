# pylint: disable=unidiomatic-typecheck

# for composite strategy
# pylint: disable=no-value-for-parameter

import subprocess
import string
import sys

import hypothesis
import hypothesis.strategies as st
import pytest
import six

from verta._internal_utils import _pip_requirements_utils


@st.composite
def versions(draw):
    numbers = st.integers(min_value=0, max_value=(2 ** 31) - 1)

    major = draw(numbers)
    minor = draw(numbers)
    patch = draw(numbers)

    return ".".join(map(str, [major, minor, patch]))


@st.composite
def metadata(draw):
    """The "cu102" in "torch==1.8.1+cu102"."""
    # https://www.python.org/dev/peps/pep-0440/#local-version-identifiers
    alphabet = string.ascii_letters + string.digits + ".-_"
    return draw(st.text(alphabet=alphabet, min_size=1))


class TestPipRequirementsUtils:
    def test_parse_pip_freeze(self):
        req_specs = _pip_requirements_utils.get_pip_freeze()
        req_specs = _pip_requirements_utils.clean_reqs_file_lines(req_specs)

        parsed_req_specs = (
            (library, constraint, _pip_requirements_utils.parse_version(version))
            for library, constraint, version in map(
                _pip_requirements_utils.parse_req_spec, req_specs
            )
        )

        for library, constraint, parsed_version in parsed_req_specs:
            assert library != ""
            assert " " not in library

            assert constraint in _pip_requirements_utils.VER_SPEC_PATTERN.strip(
                "()"
            ).split("|")

            assert parsed_version[0] >= 0  # major
            assert parsed_version[1] >= 0  # minor
            assert parsed_version[2] >= 0  # patch
            assert isinstance(parsed_version[3], six.string_types)  # suffix

    @hypothesis.given(
        version=versions(),
        metadata=metadata(),
    )
    def test_remove_torch_metadata(self, version, metadata):
        clean_requirement = "torch=={}".format(version)
        requirement = "+".join([clean_requirement, metadata])

        requirements = [requirement]
        _pip_requirements_utils.remove_public_version_identifier(requirements)
        assert requirements != [requirement]
        assert requirements == [clean_requirement]

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
                _pip_requirements_utils.clean_reqs_file_lines(
                    _pip_requirements_utils.get_pip_freeze(),
                ),
            )
        )
