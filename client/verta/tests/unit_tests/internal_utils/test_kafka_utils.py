# -*- coding: utf-8 -*-

"""Unit tests for Kafka utilities."""

from typing import Any, Dict, List

import pytest

from verta._internal_utils import kafka


@pytest.fixture
def mock_kafka_configs_response() -> Dict[str, Any]:
    """
    Provide mocked API result from `api/v1/uac-proxy/system_admin/listKafkaConfiguration`
    with a single Kafka configuration.
    """
    return {
        "configurations": [
            {
                "id": "1232abcdefg4-567h-891i-0jkl-1m1n1o2131pq",
                "kerberos": {
                    "enabled": True,
                    "conf": (
                        "[libdefaults]\ndefault_realm ="
                        " EXAMPLE.COM\n\n[realms]\nEXAMPLE.COM"
                        " = {\n    kdc_ports = 12,345\n    kadmind_port = 123\n    kdc ="
                        " testing--kafka-server-X.integrations--kafka-server-headless.ci.svc.cluster.local\n"
                        "    admin_server ="
                        " testing--kafka-server-X.integrations--kafka-server-headless.ci.svc.cluster.local\n}"
                    ),
                    "keytab": "SLG45KJlksajdfglakjWlskadfj098lkjakdfjl1k3kjflakdj-lakf+alkqfvmLPWZNVTdfS84UHads3dlkfj97alfkj!lakdfjlkaj8fg=",
                    "client_name": "test-kafka-client_name",
                    "service_name": "test-kafka-utils",
                },
                "brokerAddresses": "integrations--kafka-X.integrations--kafka-headless.ci.svc.cluster.local:00001",
                "enabled": True,
                "name": "Test Kafka Name",
            }
        ]
    }


@pytest.fixture
def mock_kafka_topics() -> List[str]:
    """Provide a mock API response from `api/v1/deployment/list/kafka-topics`."""
    return ["test-topic-1", "test-topic-2", "test-topic-3"]


def test_list_kafka_configurations(
    mocked_responses, mock_conn, mock_kafka_configs_response
):
    """Verify that list_kafka_configurations function makes the expected calls
    and handles results correctly.
    """
    url = "https://test_socket/api/v1/uac-proxy/system_admin/listKafkaConfiguration"
    mocked_responses.get(url=url, status=200, json=mock_kafka_configs_response)
    config = kafka.list_kafka_configurations(mock_conn)
    mocked_responses.assert_call_count(url, 1)
    assert config == mock_kafka_configs_response["configurations"]


def test_format_kafka_config_for_topic_search(mock_kafka_configs_response) -> None:
    """Verify that format_kafka_config_for_topic_search function formats the
    mocked kafka config into the expected results.
    """
    mock_config = mock_kafka_configs_response["configurations"][0]
    config = kafka.format_kafka_config_for_topic_search(mock_config)
    assert config == {
        "broker_addresses": [
            "integrations--kafka-X.integrations--kafka-headless.ci.svc.cluster.local:00001"
        ],
        "kerberos": {
            "enabled": True,
            "conf": (
                "[libdefaults]\ndefault_realm = EXAMPLE.COM\n\n[realms]\nEXAMPLE.COM"
                " = {\n    kdc_ports = 12,345\n    kadmind_port = 123\n    kdc ="
                " testing--kafka-server-X.integrations--kafka-server-headless.ci.svc.cluster.local\n"
                "    admin_server ="
                " testing--kafka-server-X.integrations--kafka-server-headless.ci.svc.cluster.local\n}"
            ),
            "keytab": "SLG45KJlksajdfglakjWlskadfj098lkjakdfjl1k3kjflakdj-lakf+alkqfvmLPWZNVTdfS84UHads3dlkfj97alfkj!lakdfjlkaj8fg=",
            "client_name": "test-kafka-client_name",
            "service_name": "test-kafka-utils",
        },
    }


def test_list_kafka_topics(
    mocked_responses, mock_conn, mock_kafka_configs_response, mock_kafka_topics
) -> None:
    """Verify that list_kafka_topics function makes the expected calls
    and handles results correctly.
    """
    url = "https://test_socket/api/v1/deployment/list/kafka-topics"
    mocked_responses.post(url=url, status=200, json=mock_kafka_topics)
    topics = kafka.list_kafka_topics(
        conn=mock_conn, kafka_config=mock_kafka_configs_response["configurations"][0]
    )
    mocked_responses.assert_call_count(url, 1)
    assert topics == mock_kafka_topics
