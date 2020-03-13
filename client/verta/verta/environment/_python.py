# -*- coding: utf-8 -*-

from __future__ import print_function

import copy
import sys

from ..external import six

from .._protos.public.modeldb.versioning import Environment_pb2 as _EnvironmentService

from .._internal_utils import _pip_requirements_utils

from . import _environment


class Python(_environment._Environment):
    """
    Captures metadata about Python, installed packages, and system environment variables.

    Parameters
    ----------
    requirements : list of str, optional
        List of PyPI package names. If not provided, all packages currently installed through
        pip will be captured.
    constraints : list of str, optional
        List of PyPI package names with version specifiers. If not provided, nothing will be
        captured.
    env_vars : list of str, optional
        Names of environment variables to capture. If not provided, nothing will be captured.

    Examples
    --------
    .. code-block:: python

        from verta.environment import Python
        env1 = Python(requirements=Python.read_pip_file("../requirements.txt"))
        env2 = Python(
            requirements=["tensorflow"],
            env_vars=["CUDA_VISIBLE_DEVICES"],
        )

    """
    def __init__(self, requirements=None, constraints=None, env_vars=None):
        super(Python, self).__init__(env_vars=env_vars)
        self._capture_python_version()
        self._capture_requirements(requirements)
        self._capture_constraints(constraints)

    @staticmethod
    def _req_spec_to_msg(req_spec):
        """
        Converts a requirement specifier into a protobuf message.

        Parameters
        ----------
        req_spec : str
            e.g. "banana >= 3.6.0"

        Returns
        -------
        msg : PythonRequirementEnvironmentBlob

        """
        library, constraint, version = _pip_requirements_utils.parse_req_spec(req_spec)
        major, minor, patch, suffix = _pip_requirements_utils.parse_version(version)

        req_blob_msg = _EnvironmentService.PythonRequirementEnvironmentBlob()
        req_blob_msg.library = library
        req_blob_msg.constraint = constraint
        req_blob_msg.version.major = major
        req_blob_msg.version.minor = minor
        req_blob_msg.version.patch = patch
        req_blob_msg.version.suffix = suffix

        return req_blob_msg

    def _capture_python_version(self):
        self._msg.python.version.major = sys.version_info.major
        self._msg.python.version.minor = sys.version_info.minor
        self._msg.python.version.patch = sys.version_info.micro

    def _capture_requirements(self, requirements):
        if requirements is None:
            # TODO: support conda
            req_specs = self.read_pip_environment()
        elif (isinstance(requirements, list)
              and all(isinstance(req, six.string_types) for req in requirements)):
            req_specs = copy.copy(requirements)
            _pip_requirements_utils.strip_inexact_specifiers(req_specs)
            _pip_requirements_utils.set_version_pins(req_specs)
        else:
            raise TypeError("`requirements` must be list of str,"
                            " not {}".format(type(requirements)))

        self._msg.python.requirements.extend(
            self._req_spec_to_msg(req_spec)
            for req_spec
            in req_specs
        )

    def _capture_constraints(self, constraints):
        if constraints is None:
            return
        elif (isinstance(constraints, list)
              and all(isinstance(req, six.string_types) for req in constraints)):
            req_specs = copy.copy(constraints)
        else:
            raise TypeError("`constraints` must be list of str,"
                            " not {}".format(type(constraints)))

        self._msg.python.constraints.extend(
            self._req_spec_to_msg(req_spec)
            for req_spec
            in req_specs
        )

    @staticmethod
    def read_pip_file(filepath):
        """
        Reads a pip requirements file into a list that can be passed into a new :class:`Python`.

        Parameters
        ----------
        filepath : str
            Path to a pip requirements or constraints file.

        Returns
        -------
        list of str
            Requirement specifiers.

        """
        with open(filepath, 'r') as f:
            return _pip_requirements_utils.clean_reqs_file_lines(f.readlines())

    @staticmethod
    def read_pip_environment():
        """
        Reads package versions from pip into a list that can be passed into a new :class:`Python`.

        Returns
        -------
        list of str
            Requirement specifiers.

        """
        return _pip_requirements_utils.get_pip_freeze()
