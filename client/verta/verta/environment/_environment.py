# -*- coding: utf-8 -*-

from __future__ import print_function

import os
import sys

from verta.external import six
from verta import _blob

from .._protos.public.modeldb.versioning import VersioningService_pb2
from .._protos.public.modeldb.versioning import Environment_pb2 as _EnvironmentService


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

    @classmethod
    def _from_env_proto(cls, env_msg):
        """Returns `env_msg` as a client environment object.

        Parameters
        ----------
        env_msg : versioning.Environment_pb2.EnvironmentBlob
            Environment protobuf.

        Returns
        -------
        :mod:`~verta.environment`
            Environment object.

        """
        for subcls in cls.__subclasses__():
            env_type = env_msg.WhichOneof("content")
            if env_type == subcls.__name__.lower():
                return subcls._from_proto(
                    VersioningService_pb2.Blob(environment=env_msg)
                )

        raise ValueError("environment {} not recognized".format(env_type))

    def _as_env_proto(self):
        """Returns this object as an environment protobuf message.

        Returns
        -------
        env_msg : versioning.Environment_pb2.EnvironmentBlob

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
