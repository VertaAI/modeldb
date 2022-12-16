# -*- coding: utf-8 -*-

import os

import hypothesis
import hypothesis.strategies as st
import pytest

from verta._protos.public.registry import RegistryService_pb2
from verta._protos.public.modeldb.versioning import Environment_pb2
from verta._internal_utils import arg_handler
from verta.registry import DockerImage

from .. import strategies


class TestObject:
    @pytest.mark.skip("TODO: covering the other functionality is more important")
    def test_repr(self):
        raise NotImplementedError

    @hypothesis.given(
        port=st.integers(min_value=1),
        request_path=st.text(min_size=1),
        health_path=st.text(min_size=1),
        repository=st.text(min_size=1),
        tag=st.one_of(st.none(), st.text(min_size=1)),
        sha=st.one_of(st.none(), st.text(min_size=1)),
        # pylint: disable=no-value-for-parameter
        env_vars=st.one_of(st.none(), strategies.env_vars()),
    )
    def test_properties(
        self,
        port,
        request_path,
        health_path,
        repository,
        tag,
        sha,
        env_vars,
    ):
        hypothesis.assume(tag or sha)

        docker_image = DockerImage(
            port=port,
            request_path=request_path,
            health_path=health_path,
            repository=repository,
            tag=tag,
            sha=sha,
            env_vars=env_vars,
        )

        # translate expected values
        request_path = arg_handler.ensure_starts_with_slash(request_path)
        health_path = arg_handler.ensure_starts_with_slash(health_path)
        if isinstance(env_vars, list):
            env_vars = {name: os.environ[name] for name in env_vars}

        assert docker_image.port == port
        assert docker_image.request_path == request_path
        assert docker_image.health_path == health_path
        assert docker_image.repository == repository
        assert docker_image.tag == tag
        assert docker_image.sha == sha
        assert docker_image.env_vars == (env_vars or None)


class TestProto:
    @hypothesis.given(
        port=st.integers(min_value=1, max_value=2 ** (32) - 1),
        request_path=st.text(min_size=1),
        health_path=st.text(min_size=1),
        repository=st.text(min_size=1),
        tag=st.one_of(st.none(), st.text(min_size=1)),
        sha=st.one_of(st.none(), st.text(min_size=1)),
        env_vars=st.one_of(
            st.none(),
            st.dictionaries(
                keys=st.text(min_size=1),
                values=st.text(min_size=1),
            ),
        ),
    )
    def test_from_proto(
        self,
        port,
        request_path,
        health_path,
        repository,
        tag,
        sha,
        env_vars,
    ):
        hypothesis.assume(tag or sha)

        request_path = arg_handler.ensure_starts_with_slash(request_path)
        health_path = arg_handler.ensure_starts_with_slash(health_path)

        msg = RegistryService_pb2.ModelVersion()
        msg.docker_metadata.request_port = port
        msg.docker_metadata.request_path = request_path
        msg.docker_metadata.health_path = health_path
        msg.environment.docker.repository = repository
        if tag is not None:
            msg.environment.docker.tag = tag
        if sha is not None:
            msg.environment.docker.sha = sha
        if env_vars is not None:
            msg.environment.environment_variables.extend(
                [
                    Environment_pb2.EnvironmentVariablesBlob(name=name, value=value)
                    for name, value in env_vars.items()
                ]
            )

        docker_image = DockerImage._from_model_ver_proto(msg)

        assert docker_image.port == port
        assert docker_image.request_path == request_path
        assert docker_image.health_path == health_path
        assert docker_image.repository == repository
        assert docker_image.tag == tag
        assert docker_image.sha == sha
        assert docker_image.env_vars == (env_vars or None)

    @hypothesis.given(
        port=st.integers(min_value=1, max_value=2 ** (32) - 1),
        request_path=st.text(min_size=1),
        health_path=st.text(min_size=1),
        repository=st.text(min_size=1),
        tag=st.one_of(st.none(), st.text(min_size=1)),
        sha=st.one_of(st.none(), st.text(min_size=1)),
        # pylint: disable=no-value-for-parameter
        env_vars=st.one_of(st.none(), strategies.env_vars()),
    )
    def test_as_proto(
        self,
        port,
        request_path,
        health_path,
        repository,
        tag,
        sha,
        env_vars,
    ):
        hypothesis.assume(tag or sha)

        docker_image = DockerImage(
            port=port,
            request_path=request_path,
            health_path=health_path,
            repository=repository,
            tag=tag,
            sha=sha,
            env_vars=env_vars,
        )
        msg = docker_image._as_model_ver_proto()

        # translate expected values
        request_path = arg_handler.ensure_starts_with_slash(request_path)
        health_path = arg_handler.ensure_starts_with_slash(health_path)
        if isinstance(env_vars, list):
            env_vars = {name: os.environ[name] for name in env_vars}

        assert msg.docker_metadata.request_port == port
        assert msg.docker_metadata.request_path == request_path
        assert msg.docker_metadata.health_path == health_path
        assert msg.environment.docker.repository == repository
        assert msg.environment.docker.tag == (tag or "")
        assert msg.environment.docker.sha == (sha or "")
        assert {
            var.name: var.value for var in msg.environment.environment_variables
        } == (env_vars or {})

    def test_merge_proto(self, model_version, docker_image):
        model_version._fetch_with_no_cache()

        expected_msg = RegistryService_pb2.ModelVersion()
        expected_msg.MergeFrom(model_version._msg)
        expected_msg.MergeFrom(docker_image._as_model_ver_proto())

        docker_image._merge_into_model_ver_proto(model_version._msg)
        assert model_version._msg == expected_msg
