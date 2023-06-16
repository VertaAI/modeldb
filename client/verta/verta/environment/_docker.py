# -*- coding: utf-8 -*-

import logging

from verta._vendored import six

from verta._protos.public.modeldb.versioning import (
    VersioningService_pb2 as _VersioningService,
)

from . import _environment


logger = logging.getLogger(__name__)


# TODO: Add back to documentation.reassign_module() in environment/__init__.py
#       when this has user-facing functionality.
class Docker(_environment._Environment):
    """Information to identify a Docker image environment.

    .. versionadded:: 0.20.0

    Parameters
    ----------
    repository : str
        Image repository.
    tag : str, optional
        Image tag. Either this or `sha` must be provided.
    sha : str, optional
        Image ID. Either this or `tag` must be provided.
    env_vars : list of str, or dict of str to str, optional
        Environment variables. If a list of names is provided, the values will
        be captured from the current environment. If not provided, nothing
        will be captured.

    Attributes
    ----------
    repository : str
        Image repository.
    tag : str or None
        Image tag.
    sha : str or None
        Image ID.
    env_vars : dict of str to str, or None
        Environment variables.

    Examples
    --------
    .. code-block:: python

        Docker(
            repository="012345678901.dkr.ecr.apne2-az1.amazonaws.com/models/example",
            tag="example",
        )

    """

    def __init__(
        self,
        repository,
        tag=None,
        sha=None,
        env_vars=None,
    ):
        if not (tag or sha):
            raise ValueError("must at least specify either `tag` or `sha`")

        super(Docker, self).__init__(
            env_vars=env_vars,
            autocapture=False,
        )

        self._msg.docker.repository = repository
        if tag:
            self._msg.docker.tag = tag
        if sha:
            self._msg.docker.sha = sha

    def __repr__(self):
        lines = ["Docker Environment"]
        lines.append("repository: {}".format(six.ensure_str(self.repository)))
        if self.tag:
            lines.append("tag: {}".format(six.ensure_str(self.tag)))
        if self.sha:
            lines.append("sha: {}".format(six.ensure_str(self.sha)))
        if self.env_vars:
            lines.append("environment variables:")
            lines.extend(
                sorted(
                    "    {}={}".format(name, value)
                    for name, value in self.env_vars.items()
                )
            )

        return "\n    ".join(lines)

    @property
    def repository(self):
        return self._msg.docker.repository

    @property
    def tag(self):
        return self._msg.docker.tag or None

    @property
    def sha(self):
        return self._msg.docker.sha or None

    @classmethod
    def _from_proto(cls, blob_msg):
        obj = cls(
            repository=blob_msg.environment.docker.repository,
            tag=blob_msg.environment.docker.tag or None,
            sha=blob_msg.environment.docker.sha or None,
            env_vars={
                var.name: var.value
                for var in blob_msg.environment.environment_variables
            }
            or None,
        )

        return obj

    def _as_proto(self):
        blob_msg = _VersioningService.Blob()
        blob_msg.environment.CopyFrom(self._msg)

        return blob_msg
