# -*- coding: utf-8 -*-
import json
import os
from types import FunctionType

import pytest

from tests.registry.pydantic_models import InputClass
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


@pytest.fixture
def make_model_schema_file(tmp_path, monkeypatch):
    path = tmp_path / "model_schema.json"
    schema = {"input": InputClass.schema()}
    path.write_text(json.dumps(schema))
    monkeypatch.setenv("VERTA_MODEL_SCHEMA_PATH", str(path))
