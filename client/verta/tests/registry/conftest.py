# -*- coding: utf-8 -*-

import pytest

from verta.registry import DockerImage


@pytest.fixture(scope="session")
def docker_image():
    return DockerImage(
        port=5000,
        request_path="/predict_json",
        health_path="/health",
        repository="012345678901.dkr.ecr.apne2-az1.amazonaws.com/models/example",
        tag="example",
        env_vars={"CUDA_VISIBLE_DEVICES": "0,1"},
    )
