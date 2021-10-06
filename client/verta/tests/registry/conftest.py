# -*- coding: utf-8 -*-

import pytest

from verta.registry import DockerImage


@pytest.fixture(scope="session")
def docker_image():
    return DockerImage(
        port=5000,
        request_path="/predict_json",
        health_path="/health",

        repository="models/example",
        tag="example",

        env_vars={"CUDA_VISIBLE_DEVICES": "0,1"},
    )
