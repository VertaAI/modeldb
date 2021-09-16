# -*- coding: utf-8 -*-

from __future__ import print_function

import os
import sys

import six
from six.moves import collections_abc

from verta.external import six
from verta import _blob

from .._protos.public.modeldb.versioning import Environment_pb2 as _EnvironmentService


class _Environment(_blob.Blob):
    """
    Base class for environment versioning. Not for human consumption.

    Handles environment variables and command line arguments.

    Attributes
    ----------
    env_vars : dict of str to str, or None
        Environment variables.

    """

    def __init__(self, env_vars, autocapture, apt_packages=None):
        super(_Environment, self).__init__()

        # TODO: don't use proto to store data
        self._msg = _EnvironmentService.EnvironmentBlob()

        if env_vars:
            self.add_env_vars(env_vars)
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

    @property
    def env_vars(self):
        return {
            var.name: var.value for var in self._msg.environment_variables
        } or None

    def _as_env_proto(self):
        """Returns this environment blob as an environment protobuf message.

        Returns
        -------
        env_msg : _EnvironmentService.EnvironmentBlob

        """
        return self._msg

    def add_env_vars(self, env_vars):
        """Add environment variables.

        Parameters
        ----------
        env_vars : list of str, or dict of str to str
            Environment variables. If a list of names is provided, the values will
            be captured from the current environment.

        Examples
        --------
        .. code-block:: python

            print(env.env_vars)
            # {}

            env.add_env_vars(["VERTA_HOST"])
            print(env.env_vars)
            # {'VERTA_HOST': 'app.verta.ai'}

            env.add_env_vars({"CUDA_VISIBLE_DEVICES": "0,1"})
            print(env.env_vars)
            # {'VERTA_HOST': 'app.verta.ai', 'CUDA_VISIBLE_DEVICES': '0,1'}

        """
        if isinstance(env_vars, collections_abc.Mapping):
            # as mapping
            env_vars_dict = dict(env_vars)
        else:
            # as sequence
            try:
                env_vars_dict = {name: os.environ[name] for name in env_vars}
            except KeyError as e:
                new_e = KeyError("'{}' not found in environment".format(
                    six.ensure_str(e.args[0]),
                ))
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
