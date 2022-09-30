# -*- coding: utf-8 -*-

import json

import pytest

from verta.environment import Python
from verta.registry import DockerImage
from verta.utils import ModelAPI
from verta._internal_utils import _artifact_utils


class TestDocker:
    def test_log_get(self, model_version, docker_image):
        model_version.log_docker(docker_image)

        assert model_version.get_docker() == docker_image

    def test_conflict(self, registered_model, docker_image):
        # with environment
        model_ver = registered_model.create_version()
        model_ver.log_environment(Python([]))
        with pytest.raises(ValueError, match="environment already exists;"):
            model_ver.log_docker(docker_image)

        # with docker image
        model_ver = registered_model.create_version()
        model_ver.log_docker(docker_image)
        with pytest.raises(
            ValueError,
            match="Docker image information already exists;",
        ):
            model_ver.log_docker(docker_image)

    def test_overwrite(self, model_version, docker_image):
        # with environment
        env = Python([])
        model_version.log_environment(env)
        assert model_version.get_environment() == env
        model_version.log_docker(docker_image, overwrite=True)
        assert model_version.get_environment() != env
        assert model_version.get_docker() == docker_image

        # with docker image
        docker_image_msg = docker_image._as_model_ver_proto()
        docker_image_msg.docker_metadata.request_port *= 2
        new_docker_image = DockerImage._from_model_ver_proto(docker_image_msg)
        assert new_docker_image != docker_image
        model_version.log_docker(new_docker_image, overwrite=True)
        assert model_version.get_docker() == new_docker_image


class TestModelApi:
    def test_log(self, model_version, docker_image):
        model_api = ModelAPI([[1, 2, 3]], [1])
        model_version.log_docker(docker_image, model_api=model_api)

        retrieved_model_api = model_version.get_artifact(_artifact_utils.MODEL_API_KEY)
        assert json.load(retrieved_model_api) == model_api.to_dict()

    def test_no_log(self, model_version, docker_image):
        model_version.log_docker(docker_image)

        with pytest.raises(
            KeyError,
            match="no artifact found with key {}".format(_artifact_utils.MODEL_API_KEY),
        ):
            model_version.get_artifact(_artifact_utils.MODEL_API_KEY)
