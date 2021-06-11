# -*- coding: utf-8 -*-

from __future__ import print_function

import os
import sys

from ..external import six

from .._protos.public.modeldb.versioning import Environment_pb2 as _EnvironmentService

from ..repository import _blob


class _Environment(_blob.Blob):
    """
    Base class for environment versioning. Not for human consumption.

    Handles environment variables and command line arguments.

    """

    def __init__(self, env_vars, autocapture, apt_packages=None):
        super(_Environment, self).__init__()

        # TODO: don't use proto to store data
        self._msg = _EnvironmentService.EnvironmentBlob()

        if env_vars is not None:
            self._capture_env_vars(env_vars)
        if autocapture:
            self._capture_cmd_line_args()
        if apt_packages:
            self.apt_packages = apt_packages

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

    def _as_env_proto(self):
        """Returns this environment blob as an environment protobuf message.

        Returns
        -------
        env_msg : _EnvironmentService.EnvironmentBlob

        """
        return self._msg

    def _capture_env_vars(self, env_vars):
        if env_vars is None:
            return

        try:
            env_vars_dict = {name: os.environ[name] for name in env_vars}
        except KeyError as e:
            new_e = KeyError("'{}' not found in environment".format(e.args[0]))
            six.raise_from(new_e, None)

        self._msg.environment_variables.extend(
            _EnvironmentService.EnvironmentVariablesBlob(name=name, value=value)
            for name, value in six.viewitems(env_vars_dict)
        )

    def _capture_cmd_line_args(self):
        if os.path.basename(sys.argv[0]) == "ipykernel_launcher.py":
            # Jupyter injects its own arguments, which a user almost certainly doesn't care for
            return

        self._msg.command_line.extend(sys.argv)
