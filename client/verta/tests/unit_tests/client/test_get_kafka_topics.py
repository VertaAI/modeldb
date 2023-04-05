# -*- coding: utf-8 -*-

"""Unit tests for Client.get_kafka_topics()"""

from hypothesis import given, HealthCheck, settings, strategies as st

from tests.unit_tests.strategies import mock_kafka_configs_response


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(
    kafka_topics=st.lists(st.text()),
    kafka_configs_response=mock_kafka_configs_response(),
)
def test_client_get_kafka_topics(
    kafka_topics, mocked_responses, mock_client, kafka_configs_response
):
    """Verify that client.get_kafka_info function makes the expected calls
    and handles results correctly.
    """
    conn = mock_client._conn
    with mocked_responses as _responses:
        configs_url = f"{conn.scheme}://{conn.socket}/api/v1/uac-proxy/system_admin/listKafkaConfiguration"
        topics_url = f"{conn.scheme}://{conn.socket}/api/v1/deployment/list/kafka-topics"
        _responses.get(url=configs_url, status=200, json=kafka_configs_response)
        _responses.post(url=topics_url, status=200, json=kafka_topics)
        topics = mock_client.get_kafka_topics()
        _responses.assert_call_count(configs_url, 1)
        _responses.assert_call_count(topics_url, 1)
        assert topics == kafka_topics
