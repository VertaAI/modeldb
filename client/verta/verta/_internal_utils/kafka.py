# -*- coding: utf-8 -*-

"""Utilities for working with Kafka."""

from typing import Any, Dict, List

from verta._internal_utils import _utils


def list_kafka_configurations(conn: _utils.Connection) -> List[Dict[str, Any]]:
    """Make an HTTP call to fetch a dict of Kafka configurations. If no
    configurations exist, an empty dict is returned by the API.  In that
    case this function wraps one in a list and returns it to maintain
    type consistency.
    """
    response = _utils.make_request(
        "GET",
        f"{conn.scheme}://{conn.socket}/api/v1/uac-proxy/system_admin/listKafkaConfiguration",
        conn,
    )
    _utils.raise_for_http_error(response)
    return response.json().get("configurations", [])


def format_kafka_config_for_topic_search(config: Dict[str, Any]) -> Dict[str, Any]:
    """Extract and format relevant data from a Kafka configuration for use
    with the `deployment/list/kafka-topics` API.

    The `kerberos` object is passed through unaltered, but `brokerAddresses`
    is altered for compatibility with the `/kafka-topics` API:
        - Key name is changed to snake case instead of camel case.
        - Type is converted `List[str]` instead of `str`
    """
    brokers: str = config.get("brokerAddresses", "")
    kerberos: Dict[str, Any] = config.get("kerberos", {})
    return {"broker_addresses": [brokers], "kerberos": kerberos}


def list_kafka_topics(
    conn: _utils.Connection, kafka_config: Dict[str, Any]
) -> List[str]:
    """Make an HTTP call to fetch a list of Kafka topics related to the given config."""
    response = _utils.make_request(
        "POST",
        f"{conn.scheme}://{conn.socket}/api/v1/deployment/list/kafka-topics",
        conn,
        json=format_kafka_config_for_topic_search(kafka_config),
    )
    _utils.raise_for_http_error(response)
    return response.json()
