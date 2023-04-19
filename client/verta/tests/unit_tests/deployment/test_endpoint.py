# -*- coding: utf-8 -*-
""" Unit tests for the Endpoint class. """

from copy import deepcopy
from typing import Any, Dict
from unittest.mock import patch

import pytest
from hypothesis import given, HealthCheck, settings

from tests.unit_tests.strategies import (
    mock_kafka_configs_response,
    mock_kafka_settings,
    mock_kafka_settings_with_config_id,
)


VERTA_CLASS: str = "verta.endpoint.Endpoint"
WORKSPACE_ID: int = 456
DEPLOYMENT_ID: int = 123
STAGE_ID: int = 654
BUILD_ID = 321
KAFKA_CONFIG_ID: int = 789


# TODO: Generate responses dynamically from swagger to ensure ongoing consistency with back-end API schemas
# Expected response from the _get_json_by_id method as of 2022-10-24
GET_JSON_BY_ID_RESPONSE: Dict[str, Any] = {
    "creator_request": {
        "custom_permission": {"collaborator_type": "READ_ONLY"},
        "description": "test_description",
        "path": "/test_path",
        "resource_visibility": "PRIVATE",
        "visibility": "PRIVATE",
    },
    "date_created": "2022-10-20T00:00:00.000Z",
    "date_updated": "2022-10-20T00:00:00.000Z",
    "full_url": "https://test_socket/api/v1/predict/test_path",
    "id": DEPLOYMENT_ID,
    "owner_id": "test_owner",
    "workspace_id": WORKSPACE_ID,
}

# Expected response from the get_status method as of 2022-10-24
GET_STATUS_RESPONSE: Dict[str, Any] = {
    "components": [
        {"build_id": 0, "message": "test_message", "ratio": 0, "status": "active"}
    ],
    "creator_request": {
        "autocreate_token": True,
        "enable_prediction_authz": False,
        "enable_prediction_tokens": True,
        "name": "test_name",
    },
    "date_created": "2022-10-20T00:00:00.000Z",
    "date_updated": "2022-10-20T00:00:00.000Z",
    "id": DEPLOYMENT_ID,
    "status": "active",
}

# Expected response from the get_access_token method as of 2022-10-24
GET_ACCESS_TOKEN_RESPONSE: str = "123-test-456-token-789"


class TestGetDeployedModel:
    # TODO: Implement a less verbose, more universal method of patching method and http calls.
    @patch(f"{VERTA_CLASS}.get_status", return_value=GET_STATUS_RESPONSE)
    @patch(f"{VERTA_CLASS}._get_json_by_id", return_value=GET_JSON_BY_ID_RESPONSE)
    @patch(f"{VERTA_CLASS}.get_access_token", return_value=GET_ACCESS_TOKEN_RESPONSE)
    def test_get_deployed_model_call_get_status(
        self,
        mock_get_access_token,
        mock_get_json_by_id,
        mock_get_status,
        mock_endpoint,
        mock_conn,
    ) -> None:
        """Verify that get_deployed_model calls the methods it should with the expected params"""
        mock_endpoint.get_deployed_model()
        mock_get_status.assert_called_once()
        mock_get_json_by_id.assert_called_once_with(
            mock_conn, WORKSPACE_ID, DEPLOYMENT_ID
        )
        mock_get_access_token.assert_called_once()

    @patch(f"{VERTA_CLASS}.get_status", return_value=GET_STATUS_RESPONSE)
    @patch(f"{VERTA_CLASS}._get_json_by_id", return_value=GET_JSON_BY_ID_RESPONSE)
    @patch(f"{VERTA_CLASS}.get_access_token", return_value=GET_ACCESS_TOKEN_RESPONSE)
    def test_get_deployed_model_with_full_url(
        self,
        mock_get_access_token,  # pass in patched methods in order
        mock_get_json_by_id,
        mock_get_status,
        mock_endpoint,
    ) -> None:
        """Verify that the get_deployed_model method returns the correct value for the full_url when it is returned
        by the get_json_by_id method.  The 'full_url' key was added to the schema of the response 10/2022.
        """
        deployed_model = mock_endpoint.get_deployed_model()
        assert (
            deployed_model.prediction_url
            == "https://test_socket/api/v1/predict/test_path"
        )

    GET_JSON_BY_ID_RESPONSE_MISSING_URL: Dict[str, Any] = deepcopy(
        GET_JSON_BY_ID_RESPONSE
    )
    GET_JSON_BY_ID_RESPONSE_MISSING_URL.pop(
        "full_url"
    )  # Remove the full url key and value from the dict.

    @patch(f"{VERTA_CLASS}.get_status", return_value=GET_STATUS_RESPONSE)
    @patch(
        f"{VERTA_CLASS}._get_json_by_id",
        return_value=GET_JSON_BY_ID_RESPONSE_MISSING_URL,
    )
    @patch(f"{VERTA_CLASS}.get_access_token", return_value=GET_ACCESS_TOKEN_RESPONSE)
    def test_get_deployed_model_missing_full_url(
        self,
        mock_get_access_token,  # pass in patched methods in order
        mock_get_json_by_id,
        mock_get_status,
        mock_endpoint,
    ) -> None:
        """Verify that the get_deployed_model method returns the correct value for the full_url when it is returned
        by the get_json_by_id method.  The 'full_url' key was added to the schema of the response 10/2022.
        """
        deployed_model = mock_endpoint.get_deployed_model()
        assert (
            deployed_model.prediction_url
            == "https://test_socket/api/v1/predict/test_path"
        )


class TestKafka:
    @settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
    @given(
        kafka_settings=mock_kafka_settings(),
        kafka_configs_response=mock_kafka_configs_response(),
    )
    def test_kafka_cluster_config_id_default(
        self,
        kafka_settings,
        kafka_configs_response,
        mock_endpoint,
        mock_conn,
        mocked_responses,
        mock_registered_model_version,
    ) -> None:
        """Verify that, while updating an endpoint, not including a `cluster_config_id`
        in the KafkaSettings results in the correct sequence of HTTP requests, including
        a call to fetch the ID of the current Kafka config by default."""
        deployment_url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/deployment"
        stages_url = f"{deployment_url}/workspace/{WORKSPACE_ID}/endpoints/{DEPLOYMENT_ID}/stages"
        put_config_url = f"{deployment_url}/stages/654/kafka"
        builds_url = f"{deployment_url}/workspace/{WORKSPACE_ID}/builds"

        get_configs_url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/uac-proxy/system_admin/listKafkaConfiguration"

        with mocked_responses as _responses:
            # Mock all HTTP requests involved
            _responses.get(
                url=stages_url, status=200, json={"stages": [{"id": STAGE_ID}]}
            )
            _responses.get(
                url=stages_url + f"/{STAGE_ID}", status=200, json={"id": STAGE_ID}
            )
            _responses.put(
                url=stages_url + f"/{STAGE_ID}/update",
                status=200,
                json={"id": STAGE_ID},
            )
            _responses.get(url=get_configs_url, status=200, json=kafka_configs_response)
            _responses.put(
                url=put_config_url,
                status=200,
                json={"update_request": {"cluster_config_id": KAFKA_CONFIG_ID}},
            )
            _responses.post(
                url=builds_url, status=200, json={"id": BUILD_ID, "status": "succeeded"}
            )

            mock_endpoint.update(
                mock_registered_model_version, kafka_settings=kafka_settings
            )
            _responses.assert_call_count(get_configs_url, 1)

    @settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
    @given(kafka_settings=mock_kafka_settings_with_config_id())
    def test_kafka_cluster_config_id_value(
        self,
        kafka_settings,
        mock_endpoint,
        mock_conn,
        mocked_responses,
        mock_registered_model_version,
    ) -> None:
        """Verify that, while updating an endpoint, the provided value for
        `cluster_config_id` is used, resulting in the correct sequence of HTTP
        requests.
        """
        deployment_url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/deployment"
        stages_url = f"{deployment_url}/workspace/{WORKSPACE_ID}/endpoints/{DEPLOYMENT_ID}/stages"
        put_config_url = f"{deployment_url}/stages/{STAGE_ID}/kafka"
        builds_url = f"{deployment_url}/workspace/{WORKSPACE_ID}/builds"

        with mocked_responses as _responses:
            # Mock all HTTP requests involved
            _responses.get(
                url=stages_url, status=200, json={"stages": [{"id": STAGE_ID}]}
            )
            _responses.put(
                url=put_config_url,
                status=200,
                json={"update_request": {"cluster_config_id": KAFKA_CONFIG_ID}},
            )
            _responses.post(
                url=builds_url, status=200, json={"id": BUILD_ID, "status": "succeeded"}
            )
            _responses.put(
                url=stages_url + f"/{STAGE_ID}/update",
                status=200,
                json={"id": STAGE_ID},
            )
            _responses.get(
                url=stages_url + f"/{STAGE_ID}", status=200, json={"id": STAGE_ID}
            )
            mock_endpoint.update(
                mock_registered_model_version, kafka_settings=kafka_settings
            )

    @settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
    @given(
        kafka_settings=mock_kafka_settings(),
        kafka_configs_response=mock_kafka_configs_response(),
    )
    def test_kafka_config_missing_config_id_exception(
        self,
        kafka_settings,
        kafka_configs_response,
        mock_endpoint,
        mock_conn,
        mocked_responses,
        mock_registered_model_version,
    ) -> None:
        """In the unlikely evert the ID of a found Kafka config is missing from the
        backend response, the expected exception is raised.
        """
        kafka_configs_response["configurations"][0].pop("id")
        deployment_url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/deployment"
        stages_url = f"{deployment_url}/workspace/{WORKSPACE_ID}/endpoints/{DEPLOYMENT_ID}/stages"
        get_configs_url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/uac-proxy/system_admin/listKafkaConfiguration"

        with mocked_responses as _responses:
            # Mock all HTTP requests before bad config is encountered
            _responses.get(
                url=stages_url, status=200, json={"stages": [{"id": STAGE_ID}]}
            )
            _responses.get(url=get_configs_url, status=200, json=kafka_configs_response)
            with pytest.raises(RuntimeError) as err:
                mock_endpoint.update(
                    mock_registered_model_version, kafka_settings=kafka_settings
                )
            assert (
                str(err.value)
                == "active Kafka configuration is missing its ID; please notify the Verta"
                " development team"
            )

    @settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
    @given(kafka_settings=mock_kafka_settings())
    def test_no_kafka_configs_found_exception(
        self,
        kafka_settings,
        mock_endpoint,
        mock_conn,
        mocked_responses,
        mock_registered_model_version,
    ) -> None:
        """If no valid Kafka configurations are found, the expected exception is raised."""
        deployment_url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/deployment"
        stages_url = f"{deployment_url}/workspace/{WORKSPACE_ID}/endpoints/{DEPLOYMENT_ID}/stages"
        get_configs_url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/uac-proxy/system_admin/listKafkaConfiguration"

        with mocked_responses as _responses:
            # Mock all HTTP requests before lack of Kafka configs is encountered
            _responses.get(
                url=stages_url, status=200, json={"stages": [{"id": STAGE_ID}]}
            )
            _responses.get(url=get_configs_url, status=200, json={"configurations": []})
            with pytest.raises(RuntimeError) as err:
                mock_endpoint.update(
                    mock_registered_model_version, kafka_settings=kafka_settings
                )
            assert (
                str(err.value)
                == "no Kafka configuration found; please ensure that Kafka is configured in"
                " Verta"
            )
