# -*- coding: utf-8 -*-

import logging
import re
import sys

import pytest

import cloudpickle
import six

from google.protobuf import json_format

import verta
from verta.environment import (
    Python,
)
from verta._internal_utils._pip_requirements_utils import (
    SPACY_MODEL_PATTERN,
    get_pip_freeze,
    pin_verta_and_cloudpickle,
)


def assert_parsed_reqs_match(parsed_reqs, original_reqs):
    """Assert that requirements match as expected

    ``pip freeze`` can return ``black==21.6b0`` while our parsing yields
    ``black==21.6.0b0``, though these are equivalent.

    Parameters
    ----------
    parsed_reqs : list of str
        e.g. ``Python.requirements``
    original_reqs : list of str
        e.g. ``Python.read_pip_environment()``

    """
    parsed_reqs = set(parsed_reqs)
    original_reqs = set(original_reqs)

    parsed_mapping = {
        req.split("==")[0]: Python._req_spec_to_msg(req) for req in parsed_reqs
    }
    original_mapping = {
        req.split("==")[0]: Python._req_spec_to_msg(req) for req in original_reqs
    }

    assert parsed_mapping == original_mapping


class TestObject:
    def test_repr(self):
        requirements = ["pytest=={}".format(pytest.__version__)]
        constraints = ["six=={}".format(six.__version__)]
        env_vars = ["HOME"]

        env = Python(
            requirements=requirements,
            constraints=constraints,
            env_vars=env_vars,
        )

        requirements = pin_verta_and_cloudpickle(requirements)
        for line in requirements:
            assert line in repr(env)
        for line in constraints:
            assert line in repr(env)
        for line in env_vars:
            assert line in repr(env)

    def test_raw_repr(self):
        requirements = [
            "-e git+https://github.com/matplotlib/matplotlib.git@master#egg=matplotlib",
        ]
        constraints = ["pytest > 6; python_version >= '3.8'"]

        env = Python(
            requirements=requirements,
            constraints=constraints,
        )

        assert env._msg.python.raw_requirements
        assert env._msg.python.raw_constraints

        requirements = pin_verta_and_cloudpickle(requirements)
        for line in requirements:
            assert line in repr(env)
        for line in constraints:
            assert line in repr(env)

    def test_no_autocapture(self):
        env_ver = Python(requirements=[], _autocapture=False)

        # protobuf message is empty
        assert not json_format.MessageToDict(
            env_ver._msg,
            including_default_value_fields=False,
        )


class TestReadPipEnvironment:
    @pytest.mark.skipif(
        not any(re.match(SPACY_MODEL_PATTERN + "==", req) for req in get_pip_freeze()),
        reason="requires spaCy model pinned in environment (`python -m spacy download en_core_web_sm` with pip<20)",
    )
    def test_skip_spacy_models(self):
        pattern = SPACY_MODEL_PATTERN + "=="
        requirements = Python.read_pip_environment()

        assert not any(re.match(pattern, req) for req in requirements)


class TestPythonVersion:
    def test_py_ver(self):
        env = Python(requirements=[])

        assert env._msg.python.version.major == sys.version_info.major
        assert env._msg.python.version.minor == sys.version_info.minor
        assert env._msg.python.version.patch == sys.version_info.micro


class TestAptPackages:
    def test_apt_packages(self):
        env = Python([])
        assert len(env.apt_packages) == 0

        proto_with_empty_apt = env._as_env_proto()
        assert len(proto_with_empty_apt.apt.packages) == 0

        env.apt_packages = ["opencv"]
        assert env.apt_packages == ["opencv"]
        proto = env._as_env_proto()
        assert list(proto.apt.packages) == ["opencv"]

        env.apt_packages = None
        proto_with_empty_apt = env._as_env_proto()
        assert len(proto_with_empty_apt.apt.packages) == 0

        env_initialized = Python([], apt_packages=["opencv"])
        assert env_initialized.apt_packages == ["opencv"]


class TestParsedRequirements:
    def test_from_env(self):
        reqs = Python.read_pip_environment(
            skip_options=True,
        )
        env = Python(requirements=reqs)
        assert env._msg.python.requirements
        assert not env._msg.python.raw_requirements

        reqs = pin_verta_and_cloudpickle(reqs)
        assert_parsed_reqs_match(env.requirements, reqs)

    def test_from_files(self, requirements_file):
        reqs = Python.read_pip_file(requirements_file.name)
        env = Python(requirements=reqs)
        assert env._msg.python.requirements
        assert not env._msg.python.raw_requirements

        reqs = pin_verta_and_cloudpickle(reqs)
        assert_parsed_reqs_match(env.requirements, reqs)

    def test_legacy_no_unsupported_lines(
        self, requirements_file_with_unsupported_lines
    ):
        """Unsupported lines are filtered out with legacy `skip_options=True`"""
        reqs = Python.read_pip_file(
            requirements_file_with_unsupported_lines.name,
            skip_options=True,
        )
        env = Python(requirements=reqs)
        requirements = {req.library for req in env._msg.python.requirements}

        # only has injected requirements
        assert requirements == {"verta", "cloudpickle"}

    @pytest.mark.skip(
        reason="environment versioning fails for locally-installed verta (VUMM-199)"
    )
    def test_from_file_no_versions(self, requirements_file_without_versions):
        reqs = Python.read_pip_file(requirements_file_without_versions.name)
        env = Python(requirements=reqs)
        assert env._msg.python.requirements
        assert not env._msg.python.raw_requirements

        parsed_libraries = set(req.split("==")[0] for req in env.requirements)
        assert parsed_libraries == set(reqs) | {"verta", "cloudpickle"}

    def test_torch_no_suffix(self):
        # NOTE: this test takes too long for Hypothesis
        requirement = "torch==1.8.1+cu102"
        env_ver = Python([requirement])
        assert requirement not in env_ver.requirements
        assert requirement.split("+")[0] in env_ver.requirements

    def test_torch_no_suffix_autocapture(self):
        torch = pytest.importorskip("torch")
        version = torch.__version__

        if "+" not in version:
            pytest.skip("no metadata on version number")

        requirement = "torch=={}".format(version)
        env_ver = Python(["torch"])
        assert requirement not in env_ver.requirements
        assert requirement.split("+")[0] in env_ver.requirements

    def test_inject_verta_cloudpickle(self):
        env = Python(requirements=["pytest"])
        requirements = {req.library for req in env._msg.python.requirements}

        assert "verta" in requirements
        assert "cloudpickle" in requirements


class TestRawRequirements:
    def test_unsupported_lines(self, requirements_file_with_unsupported_lines, caplog):
        """Requirements with unsupported lines get logged raw."""
        reqs = Python.read_pip_file(requirements_file_with_unsupported_lines.name)

        # each line gets logged raw
        for req in reqs:
            with caplog.at_level(logging.INFO, logger="verta"):
                env = Python(requirements=[req])

            assert (
                "failed to manually parse requirements; falling back to capturing raw contents"
                in caplog.text
            )
            caplog.clear()

            assert not env._msg.python.requirements
            assert env._msg.python.raw_requirements

            expected_reqs = pin_verta_and_cloudpickle([req])
            assert env.requirements == expected_reqs

    def test_inject_verta_cloudpickle(self):
        reqs = [
            "--no-binary :all:",
        ]
        env = Python(requirements=reqs)

        assert not env._msg.python.requirements
        assert env._msg.python.raw_requirements

        assert env.requirements == reqs + [
            "verta=={}".format(verta.__version__),
            "cloudpickle=={}".format(cloudpickle.__version__),
        ]


class TestParsedConstraints:
    @pytest.mark.skip(
        reason="environment versioning fails for locally-installed verta (VUMM-199)"
    )
    def test_from_file(self, requirements_file):
        reqs = Python.read_pip_file(requirements_file.name)
        env = Python(requirements=[], constraints=reqs)
        assert env._msg.python.constraints
        assert not env._msg.python.raw_constraints

        assert_parsed_reqs_match(env.constraints, reqs)


class TestRawConstraints:
    def test_unsupported_lines(self, requirements_file_with_unsupported_lines, caplog):
        """Constraints with unsupported lines get logged raw."""
        constraints = Python.read_pip_file(
            requirements_file_with_unsupported_lines.name
        )

        # each line gets logged raw
        for constraint in constraints:
            with caplog.at_level(logging.INFO, logger="verta"):
                env = Python(requirements=[], constraints=[constraint])

            assert (
                "failed to manually parse constraints; falling back to capturing raw contents"
                in caplog.text
            )
            caplog.clear()

            assert not env._msg.python.constraints
            assert env._msg.python.raw_constraints

            expected_constraints = [constraint]
            assert env.constraints == expected_constraints

    def test_from_file_no_versions(self, requirements_file_without_versions, caplog):
        constraints = Python.read_pip_file(requirements_file_without_versions.name)
        with caplog.at_level(logging.INFO, logger="verta"):
            env = Python(requirements=[], constraints=constraints)

        assert (
            "failed to manually parse constraints; falling back to capturing raw contents"
            in caplog.text
        )
        assert "missing its version specifier" in caplog.text

        assert not env._msg.python.constraints
        assert env._msg.python.raw_constraints

        assert (
            env._msg.python.raw_constraints == requirements_file_without_versions.read()
        )
        assert set(env.constraints) == set(constraints)


class TestVCSInstalledVerta:
    @pytest.mark.parametrize(
        "requirements",
        [
            [
                "-e git+git@github.com:VertaAI/modeldb.git@master#egg=verta&subdirectory=client/verta"
            ],
            [
                "-e git+https://github.com/VertaAI/modeldb.git@master#egg=verta&subdirectory=client/verta"
            ],
            [
                "-e git+ssh://git@github.com/VertaAI/modeldb.git@master#egg=verta&subdirectory=client/verta"
            ],
        ],
    )
    def test_vcs_installed_verta(self, requirements):
        vcs_verta_req = requirements[0]
        pinned_verta_req = "verta=={}".format(verta.__version__)

        env = Python(requirements=requirements)
        assert vcs_verta_req not in env.requirements
        assert pinned_verta_req in env.requirements
