# -*- coding: utf-8 -*-

from verta._vendored import six

from verta._protos.public.registry import RegistryService_pb2

from verta import environment
from verta._internal_utils import arg_handler


class DockerImage(object):
    """Docker image information.

    For use around :meth:`RegisteredModelVersion.log_docker() <verta.registry.entities.RegisteredModelVersion.log_docker>`.

    .. versionadded:: 0.20.0

    Parameters
    ----------
    port : int
        Container port for access.
    request_path : str
        URL path for routing predictions.
    health_path : str
        URL path for container health checks.
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
    port : int
        Container port for access.
    request_path : str
        URL path for routing predictions.
    health_path : str
        URL path for container health checks.
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

        from verta.registry import DockerImage

        docker_image = DockerImage(
            port=5000,
            request_path="/predict_json",
            health_path="/health",

            repository="012345678901.dkr.ecr.apne2-az1.amazonaws.com/models/example",
            tag="example",

            env_vars={"CUDA_VISIBLE_DEVICES": "0,1"},
        )

    """

    def __init__(
        self,
        port,
        request_path,
        health_path,
        repository,
        tag=None,
        sha=None,
        env_vars=None,
    ):
        self._port = int(port)
        self._request_path = arg_handler.ensure_starts_with_slash(request_path)
        self._health_path = arg_handler.ensure_starts_with_slash(health_path)
        self._docker_env = environment.Docker(
            repository=repository,
            tag=tag,
            sha=sha,
            env_vars=env_vars,
        )

    def __repr__(self):
        lines = ["Docker Image"]
        lines.extend(
            [
                "port: {}".format(self.port),
                "request path: {}".format(self.request_path),
                "health path: {}".format(self.health_path),
                "repository: {}".format(self.repository),
            ]
        )
        # TODO: consolidate the following lines with Docker.__repr__()
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

    def __eq__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        return self._as_model_ver_proto() == other._as_model_ver_proto()

    def __ne__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        return not self.__eq__(other)

    @property
    def port(self):
        return self._port

    @property
    def request_path(self):
        return self._request_path

    @property
    def health_path(self):
        return self._health_path

    @property
    def repository(self):
        return self._docker_env.repository

    @property
    def tag(self):
        return self._docker_env.tag

    @property
    def sha(self):
        return self._docker_env.sha

    @property
    def env_vars(self):
        return self._docker_env.env_vars

    @classmethod
    def _from_model_ver_proto(cls, model_ver_msg):
        return cls(
            port=model_ver_msg.docker_metadata.request_port,
            request_path=model_ver_msg.docker_metadata.request_path,
            health_path=model_ver_msg.docker_metadata.health_path,
            repository=model_ver_msg.environment.docker.repository,
            tag=model_ver_msg.environment.docker.tag,
            sha=model_ver_msg.environment.docker.sha,
            env_vars={
                var.name: var.value
                for var in model_ver_msg.environment.environment_variables
            },
        )

    def _as_model_ver_proto(self):
        """Return a protobuf representation of this Docker image information.

        Returns
        -------
        RegistryService_pb2.ModelVersion

        """
        return RegistryService_pb2.ModelVersion(
            docker_metadata=RegistryService_pb2.DockerMetadata(
                request_port=self._port,
                request_path=self._request_path,
                health_path=self._health_path,
            ),
            environment=self._docker_env._as_env_proto(),
        )

    def _merge_into_model_ver_proto(self, model_ver_msg):
        """Set `model_ver_msg`'s ``docker_metadata`` and ``environment``.

        Parameters
        ----------
        model_ver_msg : RegistryService_pb2.ModelVersion
            A model version's protobuf message.

        """
        model_ver_msg.docker_metadata.Clear()
        model_ver_msg.environment.Clear()
        model_ver_msg.MergeFrom(self._as_model_ver_proto())
