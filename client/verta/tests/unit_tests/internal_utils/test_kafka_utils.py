# -*- coding: utf-8 -*-

"""Unit tests for Kafka utilities."""

from typing import Any, Dict

from hypothesis import given, HealthCheck, settings, strategies as st

from verta._internal_utils import kafka


@st.composite
def mock_kafka_configs_response(draw) -> Dict[str, Any]:
    """
    Provide mocked API result from `api/v1/uac-proxy/system_admin/listKafkaConfiguration`
    with a single Kafka configuration.
    """
    return {
        "configurations": [
            {
                "id": draw(st.integers()),
                "kerberos": {
                    "enabled": draw(st.booleans()),
                    "client_name": draw(st.text()),
                    "conf": draw(st.text()),
                    "keytab": draw(st.text()),
                    "service_name": draw(st.text()),
                },
                "brokerAddresses": draw(st.text()),
                "enabled": draw(st.booleans()),
                "name": draw(st.text()),
            }
        ]
    }


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(kafka_configs_response=mock_kafka_configs_response())
def test_list_kafka_configurations(kafka_configs_response, mocked_responses, mock_conn):
    """Verify that list_kafka_configurations function makes the expected calls
    and handles results correctly.
    """
    url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/uac-proxy/system_admin/listKafkaConfiguration"
    with mocked_responses as _responses:
        _responses.get(url=url, status=200, json=kafka_configs_response)
        config = kafka.list_kafka_configurations(mock_conn)
        _responses.assert_call_count(url, 1)
        assert config == kafka_configs_response["configurations"]


@given(kafka_configs_response=mock_kafka_configs_response())
def test_format_kafka_config_for_topic_search(kafka_configs_response) -> None:
    """Verify that format_kafka_config_for_topic_search function formats the
    mocked kafka config into the expected results.
    """
    mock_config = kafka_configs_response["configurations"][0]
    config = kafka.format_kafka_config_for_topic_search(mock_config)
    assert config == {
        "broker_addresses": [mock_config["brokerAddresses"]],
        "kerberos": mock_config["kerberos"],
    }


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(
    kafka_topics=st.lists(st.text()),
    kafka_configs_response=mock_kafka_configs_response(),
)
def test_list_kafka_topics(
    kafka_topics, mocked_responses, mock_conn, kafka_configs_response
) -> None:
    """Verify that list_kafka_topics function makes the expected calls
    and handles results correctly.
    """
    url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/deployment/list/kafka-topics"
    with mocked_responses as _responses:
        _responses.post(url=url, status=200, json=kafka_topics)
        topics = kafka.list_kafka_topics(
            conn=mock_conn, kafka_config=kafka_configs_response["configurations"][0]
        )
        _responses.assert_call_count(url, 1)
        assert topics == kafka_topics
