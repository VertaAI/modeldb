# -*- coding: utf-8 -*-

""" Unit tests for the RegisteredModelVersion class. """

from string import ascii_letters
from typing import List
from unittest.mock import patch

import hypothesis
import hypothesis.strategies as st

from verta._internal_utils._utils import timestamp_to_str
from verta._protos.public.registry import RegistryService_pb2
from verta.registry.entities import RegisteredModelVersion

from ..strategies import (
    artifact_proto,
    code_blob_proto,
    int64,
    model_artifact_proto,
    uint64,
)


@st.composite
def model_ver_proto(draw) -> RegistryService_pb2.ModelVersion:
    """
    Generate a mocked ModelVersion protobuf object.

    This strategy does not yet set all available fields, but exists in its current form to cover newly-added model catalog fields.

    """
    return RegistryService_pb2.ModelVersion(
        id=draw(uint64()),
        registered_model_id=draw(uint64()),
        version=draw(st.text(ascii_letters)),

        time_updated=draw(int64()),
        time_created=draw(int64()),

        labels=draw(st.lists(st.text(ascii_letters), unique=True)),

        input_description=draw(st.text(ascii_letters)),
        output_description=draw(st.text(ascii_letters)),
        hide_input_label=draw(st.booleans()),
        hide_output_label=draw(st.booleans()),

        model=draw(st.one_of(st.none(), model_artifact_proto())),
        artifacts=draw(st.lists(artifact_proto(), unique_by=lambda artifact: artifact.key)),
        datasets=draw(st.lists(artifact_proto(), unique_by=lambda artifact: artifact.key)),
        code_blob_map=draw(st.dictionaries(st.text(ascii_letters), code_blob_proto())),
    )


@patch.object(RegisteredModelVersion, "_refresh_cache", return_value=None)
@hypothesis.given(model_ver_proto=model_ver_proto(), workspace=st.text(ascii_letters, min_size=1))
def test_repr(mock_conn, mock_config, model_ver_proto, workspace):
    """
    Verify that RegisteredModelVersion.__repr__() renders expected fields and correct values.

    This test does not yet cover all available fields, but exists in its current form to cover newly-added model catalog fields.

    """
    model_ver = RegisteredModelVersion(conn=mock_conn, conf=mock_config, msg=model_ver_proto)
    with patch.object(RegisteredModelVersion, "workspace", new=workspace):
        repr_lines: List[str] = repr(model_ver).split("\n")
    msg: RegistryService_pb2.ModelVersion = model_ver._msg

    assert f"version: {msg.version}" in repr_lines
    assert "url: {}://{}/{}/registry/{}/versions/{}".format(
        mock_conn.scheme,
        mock_conn.socket,
        workspace,
        msg.registered_model_id,
        msg.id,
    ) in repr_lines
    assert f"time created: {timestamp_to_str(msg.time_created)}" in repr_lines
    assert f"time updated: {timestamp_to_str(msg.time_updated)}" in repr_lines
    assert f"labels: {msg.labels}" in repr_lines

    assert f"input description: {msg.input_description}" in repr_lines
    assert f"output description: {msg.output_description}" in repr_lines
    assert f"hide input label: {msg.hide_input_label}" in repr_lines
    assert f"hide output label: {msg.hide_output_label}" in repr_lines

    assert f"id: {msg.id}" in repr_lines
    assert f"registered model id: {msg.registered_model_id}" in repr_lines

    expected_artifact_keys: List[str] = [artifact.key for artifact in msg.artifacts]
    if msg.model.key:
        expected_artifact_keys.append(model_ver._MODEL_KEY)
    assert f"artifact keys: {sorted(expected_artifact_keys)}" in repr_lines
    assert f"dataset version keys: {sorted(dataset.key for dataset in msg.datasets)}" in repr_lines
    assert f"code version keys: {sorted(msg.code_blob_map.keys())}" in repr_lines
