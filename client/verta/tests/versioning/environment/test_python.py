# -*- coding: utf-8 -*-

import copy
import logging
import os
import sys

import pytest

import cloudpickle
import six

from google.protobuf import json_format

import verta
from verta.environment import (
    Python,
)
from verta._internal_utils import _pip_requirements_utils


class TestPython:
    @staticmethod
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
            req.split("==")[0]: Python._req_spec_to_msg(req)
            for req in parsed_reqs
        }
        original_mapping = {
            req.split("==")[0]: Python._req_spec_to_msg(req)
            for req in original_reqs
        }

        assert parsed_mapping == original_mapping

    def test_py_ver(self):
        env = Python(requirements=[])

        assert env._msg.python.version.major == sys.version_info.major
        assert env._msg.python.version.minor == sys.version_info.minor
        assert env._msg.python.version.patch == sys.version_info.micro

    def test_env_vars(self):
        env_vars = os.environ.keys()
        env = Python(requirements=[], env_vars=env_vars)

        assert env._msg.environment_variables

    def test_reqs_from_env(self):
        reqs = Python.read_pip_environment(
            skip_options=True,
        )
        env = Python(requirements=reqs)
        assert env._msg.python.requirements
        assert not env._msg.python.raw_requirements

        _pip_requirements_utils.pin_verta_and_cloudpickle(reqs)
        self.assert_parsed_reqs_match(env.requirements, reqs)

    def test_reqs(self, requirements_file):
        reqs = Python.read_pip_file(requirements_file.name)
        env = Python(requirements=reqs)
        assert env._msg.python.requirements
        assert not env._msg.python.raw_requirements

        _pip_requirements_utils.pin_verta_and_cloudpickle(reqs)
        self.assert_parsed_reqs_match(env.requirements, reqs)

    def test_reqs_without_versions(self, requirements_file_without_versions):
        reqs = Python.read_pip_file(requirements_file_without_versions.name)
        env = Python(requirements=reqs)
        assert env._msg.python.requirements
        assert not env._msg.python.raw_requirements

        parsed_libraries = set(req.split("==")[0] for req in env.requirements)
        assert parsed_libraries == set(reqs) | {"verta", "cloudpickle"}

    def test_constraints_from_file(self, requirements_file):
        reqs = Python.read_pip_file(requirements_file.name)
        env = Python(requirements=[], constraints=reqs)
        assert env._msg.python.constraints
        assert not env._msg.python.raw_constraints

        self.assert_parsed_reqs_match(env.constraints, reqs)

    def test_constraints_from_file_no_versions_error(
        self, requirements_file_without_versions, caplog
    ):
        reqs = Python.read_pip_file(requirements_file_without_versions.name)
        with caplog.at_level(logging.WARNING, logger="verta"):
            env = Python(requirements=[], constraints=reqs)

        assert "failed to manually parse constraints; falling back to capturing raw contents" in caplog.text
        assert "missing its version specifier" in caplog.text

        assert not env._msg.python.constraints
        assert env._msg.python.raw_constraints

        assert env._msg.python.raw_constraints == requirements_file_without_versions.read()
        assert set(env.constraints) == set(reqs)

    def test_inject_verta_cloudpickle(self):
        env = Python(requirements=[])
        requirements = {req.library for req in env._msg.python.requirements}

        assert "verta" in requirements
        assert "cloudpickle" in requirements

    def test_raw_inject_verrta_cloudpickle(self):
        reqs = [
            "-e client/verta"
        ]
        env = Python(requirements=reqs)

        assert not env._msg.python.requirements
        assert env._msg.python.raw_requirements

        assert env.requirements == reqs + [
            "verta=={}".format(verta.__version__),
            "cloudpickle=={}".format(cloudpickle.__version__),
        ]

    def test_reqs_no_unsupported_lines(self, requirements_file_with_unsupported_lines):
        """Unsupported lines are filtered out with legacy `skip_options=True`"""
        reqs = Python.read_pip_file(
            requirements_file_with_unsupported_lines.name,
            skip_options=True,
        )
        env = Python(requirements=reqs)
        requirements = {req.library for req in env._msg.python.requirements}

        # only has injected requirements
        assert requirements == {"verta", "cloudpickle"}

    def test_reqs_raw_unsupported_lines(
        self, requirements_file_with_unsupported_lines, caplog
    ):
        reqs = Python.read_pip_file(requirements_file_with_unsupported_lines.name)
        with caplog.at_level(logging.WARNING, logger="verta"):
            env = Python(requirements=reqs)

        assert "failed to manually parse requirements; falling back to capturing raw contents" in caplog.text
        caplog.clear()

        assert not env._msg.python.requirements
        assert env._msg.python.raw_requirements

        expected_reqs = copy.copy(reqs)
        _pip_requirements_utils.pin_verta_and_cloudpickle(expected_reqs)
        assert env.requirements == expected_reqs

        # also verify each individual line
        for req in reqs:
            with caplog.at_level(logging.WARNING, logger="verta"):
                env = Python(requirements=[req])

            assert "failed to manually parse requirements; falling back to capturing raw contents" in caplog.text
            caplog.clear()

            assert not env._msg.python.requirements
            assert env._msg.python.raw_requirements

            expected_reqs = [req]
            _pip_requirements_utils.pin_verta_and_cloudpickle(expected_reqs)
            assert env.requirements == expected_reqs

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
