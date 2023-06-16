# -*- coding: utf-8 -*-

import copy
import logging
import os
import sys

from .._vendored import six

from .._protos.public.modeldb.versioning import (
    VersioningService_pb2 as _VersioningService,
)
from .._protos.public.modeldb.versioning import Environment_pb2 as _EnvironmentService

from .._internal_utils import _pip_requirements_utils

from . import _environment


logger = logging.getLogger(__name__)


class Python(_environment._Environment):
    """Capture metadata about Python, installed packages, and system environment variables.

    .. note::

        Comments and blank lines will not be captured during parsing.

    Parameters
    ----------
    requirements : list of str
        List of PyPI package names.
    constraints : list of str, optional
        List of PyPI package names with version specifiers. If not provided, nothing will be
        captured.
    env_vars : list of str, or dict of str to str, optional
        Environment variables. If a list of names is provided, the values will
        be captured from the current environment. If not provided, nothing
        will be captured.
    apt_packages : list of str, optional
        Apt packages to be installed alongside a Python environment.
    _autocapture : bool, default True
        Whether to enable the automatic capturing behavior of parameters above.

    Attributes
    ----------
    constraints : list of str
        pip constraints.
    requirements : list of str
        pip requirements.
    apt_packages : list of str
        Apt packages to be installed alongside a Python environment.
    env_vars : dict of str to str, or None
        Environment variables.

    Examples
    --------
    .. code-block:: python

        from verta.environment import Python
        env1 = Python(requirements=Python.read_pip_environment())
        env1.apt_packages = ["python3-opencv"]

        env2 = Python(requirements=Python.read_pip_file("../requirements.txt"))

        env3 = Python(
            requirements=[
                "scikit-learn==1.0.2",
                "tensorflow",
            ],
            env_vars=["CUDA_VISIBLE_DEVICES"],
            apt_packages=["python3-opencv"]
        )
    """

    DEFAULT_EXCLUDED_PACKAGES = [
        _pip_requirements_utils.SPACY_MODEL_PATTERN,
        "anaconda-client",
    ]

    def __init__(
        self,
        requirements,
        constraints=None,
        env_vars=None,
        apt_packages=None,
        _autocapture=True,
    ):
        super(Python, self).__init__(
            env_vars=env_vars,
            autocapture=_autocapture,
        )

        if apt_packages:
            self.apt_packages = apt_packages
        if _autocapture:
            self._capture_python_version()
        if requirements or _autocapture:
            self._capture_requirements(requirements)
        if constraints:
            self._capture_constraints(constraints)

    def __repr__(self):
        lines = ["Python Environment"]
        if self._msg.python.version.major:
            lines.append(
                "Python {}.{}.{}".format(
                    self._msg.python.version.major,
                    self._msg.python.version.minor,
                    self._msg.python.version.patch,
                )
            )
        if self._msg.python.requirements or self._msg.python.raw_requirements:
            lines.append("requirements:")
            lines.extend(map("    {}".format, self.requirements))
        if self._msg.python.constraints or self._msg.python.raw_constraints:
            lines.append("constraints:")
            lines.extend(map("    {}".format, self.constraints))
        if self.env_vars:
            lines.append("environment variables:")
            lines.extend(
                sorted(
                    "    {}={}".format(name, value)
                    for name, value in self.env_vars.items()
                )
            )
        if self.apt_packages:
            lines.append("apt packages:")
            lines.extend("    {}".format(package) for package in self.apt_packages)
        if self._msg.command_line:
            lines.append("command line arguments:")
            lines.extend("    {}".format(arg) for arg in self._msg.command_line)

        return "\n    ".join(lines)

    @property
    def apt_packages(self):
        return list(self._msg.apt.packages)

    @apt_packages.setter
    def apt_packages(self, packages):
        if packages:
            apt_blob = _EnvironmentService.AptEnvironmentBlob(packages=packages)
            self._msg.apt.CopyFrom(apt_blob)
        else:
            self._msg.apt.Clear()

    @property
    def constraints(self):
        if self._msg.python.constraints:
            # parsed constraints
            return sorted(
                map(
                    self._req_spec_msg_to_str,
                    self._msg.python.constraints,
                )
            )
        else:
            # raw constraints
            return self._msg.python.raw_constraints.splitlines()

    @property
    def requirements(self):
        if self._msg.python.requirements:
            # parsed requirements
            return sorted(
                map(
                    self._req_spec_msg_to_str,
                    self._msg.python.requirements,
                )
            )
        else:
            # raw requirements
            return self._msg.python.raw_requirements.splitlines()

    @classmethod
    def _from_proto(cls, blob_msg):
        obj = cls(requirements=[], _autocapture=False)
        obj._msg.CopyFrom(blob_msg.environment)

        return obj

    def _as_proto(self):
        blob_msg = _VersioningService.Blob()
        blob_msg.environment.CopyFrom(self._msg)

        return blob_msg

    @staticmethod
    def _req_spec_to_msg(req_spec):
        """Convert a requirement specifier into a protobuf message.

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

    @staticmethod
    def _req_spec_msg_to_str(msg):
        """Inverse of :meth:`Python._req_spec_to_msg`.

        Parameters
        ----------
        msg : PythonRequirementEnvironmentBlob

        Returns
        -------
        req_spec : str

        """
        return "{}{}{}".format(
            msg.library,
            msg.constraint,
            "{}.{}.{}{}".format(
                msg.version.major,
                msg.version.minor,
                msg.version.patch,
                msg.version.suffix,
            ),
        )

    def _capture_python_version(self):
        self._msg.python.version.major = sys.version_info.major
        self._msg.python.version.minor = sys.version_info.minor
        self._msg.python.version.patch = sys.version_info.micro

    def _capture_requirements(self, requirements):
        if not (
            isinstance(requirements, list)
            and all(isinstance(req, six.string_types) for req in requirements)
        ):
            raise TypeError(
                "`requirements` must be list of str,"
                " not {}".format(type(requirements))
            )

        requirements = _pip_requirements_utils.pin_verta_and_cloudpickle(requirements)

        try:
            requirements_copy = copy.copy(requirements)
            requirements_copy = _pip_requirements_utils.clean_reqs_file_lines(
                requirements_copy,
                ignore_unsupported=False,
            )
            _pip_requirements_utils.must_all_valid_package_names(requirements_copy)
            _pip_requirements_utils.strip_inexact_specifiers(requirements_copy)
            _pip_requirements_utils.set_version_pins(requirements_copy)
            _pip_requirements_utils.remove_local_version_identifier(requirements_copy)

            self._msg.python.requirements.extend(
                map(
                    self._req_spec_to_msg,
                    requirements_copy,
                ),
            )
        except:
            logger.info(
                "failed to manually parse requirements;"
                " falling back to capturing raw contents",
                exc_info=True,
            )
            self._msg.python.raw_requirements = "\n".join(requirements)

    def _capture_constraints(self, constraints):
        if constraints is None:
            return
        elif not (
            isinstance(constraints, list)
            and all(isinstance(req, six.string_types) for req in constraints)
        ):
            raise TypeError(
                "`constraints` must be list of str," " not {}".format(type(constraints))
            )

        try:
            constraints_copy = copy.copy(constraints)
            constraints_copy = _pip_requirements_utils.clean_reqs_file_lines(
                constraints_copy,
                ignore_unsupported=False,
            )
            self._msg.python.constraints.extend(
                map(
                    self._req_spec_to_msg,
                    constraints_copy,
                ),
            )
        except:
            logger.info(
                "failed to manually parse constraints;"
                " falling back to capturing raw contents",
                exc_info=True,
            )
            self._msg.python.raw_constraints = "\n".join(constraints)

    @staticmethod
    def read_pip_file(filepath, skip_options=False):
        """Read a pip requirements or constraints file into a list.

        .. versionchanged:: 0.20.0

            This method now includes, rather than skipping, pip packages
            installed via version control systems, ``pip install`` options,
            and other requirements file configurations. The new `skip_options`
            parameter can be set to ``True`` to restore the old behavior.

        Parameters
        ----------
        filepath : str
            Path to a pip requirements or constraints file.
        skip_options : bool, default False
            Whether to omit lines with advanced pip options.

        Returns
        -------
        list of str
            pip requirement specifiers and options.

        Examples
        --------
        .. code-block:: python

            from verta.environment import Python

            env = Python(
                requirements=Python.read_pip_file("requirements.txt"),
                constraints=Python.read_pip_file("constraints.txt"),
            )

        """
        filepath = os.path.expanduser(filepath)
        with open(filepath, "r") as f:
            requirements = f.read().splitlines()
        if skip_options:
            requirements = _pip_requirements_utils.clean_reqs_file_lines(requirements)

        return requirements

    @staticmethod
    def read_pip_environment(
        skip_options=False,
        exclude=DEFAULT_EXCLUDED_PACKAGES,
    ):
        """Read package versions from pip into a list.

        .. versionchanged:: 0.20.0

            This method now includes, rather than skipping, pip packages
            installed via version control systems. The new `skip_options`
            parameter can be set to ``True`` to restore the old behavior.

        Parameters
        ----------
        skip_options : bool, default False
            Whether to omit lines with advanced pip options.
        exclude : list of str, default spaCy models and "anaconda-client"
            Regex patterns for packages to omit. Certain tools, such as conda
            and spaCy, install items into the pip environment that can't be
            installed from PyPI when a model is deployed in Verta.

        Returns
        -------
        list of str
            pip requirement specifiers.

        Examples
        --------
        .. code-block:: python

            from verta.environment import Python

            env = Python(Python.read_pip_environment())

        """
        requirements = _pip_requirements_utils.get_pip_freeze()
        if skip_options:
            requirements = _pip_requirements_utils.clean_reqs_file_lines(
                requirements,
            )
        if exclude:
            requirements = _pip_requirements_utils.remove_pinned_requirements(
                requirements,
                exclude,
            )

        return requirements
