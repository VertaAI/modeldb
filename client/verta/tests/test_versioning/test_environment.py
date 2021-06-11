# -*- coding: utf-8 -*-

# for composite strategy
# pylint: disable=no-value-for-parameter

import os
import string
import subprocess
import sys
import tempfile

import hypothesis
import hypothesis.strategies as st
import pytest
import six

from google.protobuf import json_format

from verta.environment import (
    Python,
)
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


@pytest.fixture
def requirements_file_without_versions():
    with tempfile.NamedTemporaryFile("w+") as tempf:
        # create requirements file from pip freeze
        pip_freeze = subprocess.check_output([sys.executable, "-m", "pip", "freeze"])
        pip_freeze = six.ensure_str(pip_freeze)
        stripped_pip_freeze = "\n".join(
            line.split("==")[0] for line in pip_freeze.splitlines()
        )
        tempf.write(stripped_pip_freeze)
        tempf.flush()  # flush object buffer
        os.fsync(tempf.fileno())  # flush OS buffer
        tempf.seek(0)

        yield tempf


@pytest.fixture
def requirements_file_with_unsupported_lines():
    with tempfile.NamedTemporaryFile("w+") as tempf:
        requirements = [
            "",
            "# this is a comment",
            "--no-binary :all:",
            "--only-binary :none:",
            "--require-hashes",
            "--pre",
            "--trusted-host localhost:3000",
            "-c some_constraints.txt",
            "-f file://dummy",
            "-i https://pypi.org/simple",
            "-e git+git@github.com:VertaAI/modeldb.git@master#egg=verta&subdirectory=client/verta",
            "-r more_requirements.txt",
            "en-core-web-sm==2.2.5",
        ]
        tempf.write("\n".join(requirements))
        tempf.flush()  # flush object buffer
        os.fsync(tempf.fileno())  # flush OS buffer
        tempf.seek(0)

        yield tempf


class TestUtils:
    def test_parse_pip_freeze(self):
        req_specs = _pip_requirements_utils.get_pip_freeze()
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


class TestPython:
    def test_py_ver(self):
        env = Python(requirements=[])

        assert env._msg.python.version.major == sys.version_info.major
        assert env._msg.python.version.minor == sys.version_info.minor
        assert env._msg.python.version.patch == sys.version_info.micro

    def test_env_vars(self):
        env_vars = os.environ.keys()
        env = Python(requirements=[], env_vars=env_vars)

        assert env._msg.environment_variables

    def test_commit(self, commit):
        env = Python(requirements=[])

        commit.update("env", env)
        commit.save(message="banana")
        assert commit.get("env")

    def test_reqs_from_env(self):
        reqs = Python.read_pip_environment()
        env = Python(requirements=reqs)
        assert env._msg.python.requirements

    def test_reqs(self, requirements_file):
        reqs = Python.read_pip_file(requirements_file.name)
        env = Python(requirements=reqs)
        assert env._msg.python.requirements

    def test_reqs_without_versions(self, requirements_file_without_versions):
        reqs = Python.read_pip_file(requirements_file_without_versions.name)
        env = Python(requirements=reqs)
        assert env._msg.python.requirements

    def test_constraints_from_file(self, requirements_file):
        reqs = Python.read_pip_file(requirements_file.name)
        env = Python(requirements=[], constraints=reqs)
        assert env._msg.python.constraints

    def test_constraints_from_file_no_versions_error(
        self, requirements_file_without_versions
    ):
        reqs = Python.read_pip_file(requirements_file_without_versions.name)
        with pytest.raises(ValueError):
            Python(requirements=[], constraints=reqs)

    def test_inject_verta_cloudpickle(self):
        env = Python(requirements=[])
        requirements = {req.library for req in env._msg.python.requirements}

        assert "verta" in requirements
        assert "cloudpickle" in requirements

    def test_reqs_no_unsupported_lines(self, requirements_file_with_unsupported_lines):
        reqs = Python.read_pip_file(requirements_file_with_unsupported_lines.name)
        env = Python(requirements=reqs)
        requirements = {req.library for req in env._msg.python.requirements}

        # only has injected requirements
        assert requirements == {"verta", "cloudpickle"}

    def test_no_autocapture(self):
        env_ver = Python(requirements=[], _autocapture=False)

        # protobuf message is empty
        assert not json_format.MessageToDict(
            env_ver._msg,
            including_default_value_fields=False,
        )

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

    def test_repr(self):
        """Tests that __repr__() executes without error"""
        env_ver = Python(
            requirements=["pytest=={}".format(pytest.__version__)],
            constraints=["six=={}".format(six.__version__)],
            env_vars=["HOME"],
        )

        assert env_ver.__repr__()

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
