# -*- coding: utf-8 -*-

""" Unit tests for the RegisteredModelVersion class. """

from string import ascii_letters
from typing import List, Optional
from unittest.mock import patch

from hypothesis import given, settings, HealthCheck
import hypothesis.strategies as st
import responses
from responses.matchers import query_param_matcher

from verta._internal_utils._utils import timestamp_to_str
from verta._protos.public.registry import RegistryService_pb2
from verta.registry.entities import RegisteredModelVersion

from ..strategies import (
    artifact_proto,
    attribute_proto,
    code_blob_proto,
    int64,
    model_artifact_proto,
    uint64,
)


@st.composite
def model_ver_proto(
    draw,
    allow_attributes: bool = True,
    allow_artifacts: bool = True,
    with_model: Optional[bool] = None,
    with_experiment_run_id: bool = False,
) -> RegistryService_pb2.ModelVersion:
    """Generate a mocked ModelVersion protobuf object.

    This strategy does not yet set all available fields, but exists in its
    current form to cover newly-added model catalog fields.

    Parameters
    ----------
    allow_attributes : bool, default True
        Whether to sometimes set the ``attributes`` field.
    allow_artifacts : bool, default True
        Whether to sometimes set the ``artifacts`` field.
    with_model : bool, optional
        Whether to set ``model`` field. Default behavior is to sometimes set
        it and sometimes not.
    with_experiment_run_id : bool, default False
        Whether to set ``experiment_run_id`` field.

    """
    msg = RegistryService_pb2.ModelVersion(
        # creation metadata
        id=draw(uint64()),
        registered_model_id=draw(uint64()),
        version=draw(st.text(ascii_letters)),
        time_updated=draw(int64()),
        time_created=draw(int64()),
        # user-specified metadata
        labels=draw(st.lists(st.text(ascii_letters), unique=True)),
        # I/O descriptions
        input_description=draw(st.text(ascii_letters)),
        output_description=draw(st.text(ascii_letters)),
        hide_input_label=draw(st.booleans()),
        hide_output_label=draw(st.booleans()),
        # artifacts
        datasets=draw(
            st.lists(artifact_proto(), unique_by=lambda artifact: artifact.key),
        ),
        code_blob_map=draw(st.dictionaries(st.text(ascii_letters), code_blob_proto())),
    )
    if allow_attributes:
        msg.attributes.extend(
            draw(
                st.lists(attribute_proto(), unique_by=lambda attribute: attribute.key),
            )
        )
    if allow_artifacts:
        msg.artifacts.extend(
            draw(
                st.lists(artifact_proto(), unique_by=lambda artifact: artifact.key),
            ),
        )
    if with_model or (with_model is None and draw(st.booleans())):
        msg.model.CopyFrom(draw(model_artifact_proto()))
    if with_experiment_run_id:
        msg.experiment_run_id = str(draw(st.uuids()))

    return msg


@given(
    model_ver_proto=model_ver_proto(),
    workspace=st.text(ascii_letters, min_size=1),
)
def test_repr(mock_conn, mock_config, model_ver_proto, workspace):
    """
    Verify that RegisteredModelVersion.__repr__() renders expected fields and correct values.

    This test does not yet cover all available fields, but exists in its current form to cover newly-added model catalog fields.

    """
    with patch.object(RegisteredModelVersion, "_refresh_cache", return_value=None):
        model_ver = RegisteredModelVersion(
            conn=mock_conn,
            conf=mock_config,
            msg=model_ver_proto,
        )
        with patch.object(RegisteredModelVersion, "workspace", new=workspace):
            repr_lines: List[str] = repr(model_ver).split("\n")
        msg: RegistryService_pb2.ModelVersion = model_ver._msg

        assert f"version: {msg.version}" in repr_lines
        assert (
            "url: {}://{}/{}/registry/{}/versions/{}".format(
                mock_conn.scheme,
                mock_conn.socket,
                workspace,
                msg.registered_model_id,
                msg.id,
            )
            in repr_lines
        )
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
        assert (
            f"dataset version keys: {sorted(dataset.key for dataset in msg.datasets)}"
            in repr_lines
        )
        assert f"code version keys: {sorted(msg.code_blob_map.keys())}" in repr_lines


@settings(
    # this test generates *two* model versions!
    suppress_health_check=[HealthCheck.data_too_large, HealthCheck.too_slow],
)
@given(
    model_ver_proto=model_ver_proto(with_experiment_run_id=False),
    model_ver_from_run_proto=model_ver_proto(with_experiment_run_id=True),
)
def test_experiment_run_id_property(
    mock_conn,
    mock_config,
    model_ver_proto,
    model_ver_from_run_proto,
):
    """Verify ``ModelVersion.experiment_run_id`` has the expected value."""
    with patch.object(RegisteredModelVersion, "_refresh_cache", return_value=None):
        model_ver = RegisteredModelVersion(
            conn=mock_conn,
            conf=mock_config,
            msg=model_ver_proto,
        )
        assert model_ver.experiment_run_id is None

        model_ver_from_run = RegisteredModelVersion(
            conn=mock_conn,
            conf=mock_config,
            msg=model_ver_from_run_proto,
        )
        assert (
            model_ver_from_run.experiment_run_id
            == model_ver_from_run_proto.experiment_run_id
        )


@settings(
    # this test generates *two* model versions!
    suppress_health_check=[HealthCheck.data_too_large, HealthCheck.too_slow],
)
@given(
    model_ver_proto=model_ver_proto(with_experiment_run_id=False),
    model_ver_from_run_proto=model_ver_proto(with_experiment_run_id=True),
)
def test_get_experiment_run(
    mock_conn,
    mock_config,
    model_ver_proto,
    model_ver_from_run_proto,
):
    """Verify ``ModelVersion.get_experiment_run`` makes the expected backend call."""
    with patch.object(RegisteredModelVersion, "_refresh_cache", return_value=None):
        model_ver = RegisteredModelVersion(
            conn=mock_conn,
            conf=mock_config,
            msg=model_ver_proto,
        )
        assert model_ver.get_experiment_run() is None

        model_ver_from_run = RegisteredModelVersion(
            conn=mock_conn,
            conf=mock_config,
            msg=model_ver_from_run_proto,
        )
        with responses.RequestsMock() as rsps:
            rsps.get(
                f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/modeldb/experiment-run/getExperimentRunById",
                match=[
                    query_param_matcher(
                        {"id": model_ver_from_run_proto.experiment_run_id},
                    ),
                ],
                status=200,
                json={
                    "experiment_run": {
                        "id": model_ver_from_run_proto.experiment_run_id,
                    },
                },
            )

            assert (
                model_ver_from_run.get_experiment_run().id
                == model_ver_from_run_proto.experiment_run_id
            )
