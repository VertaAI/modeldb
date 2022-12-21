# -*- coding: utf-8 -*-
""" Unit tests for the Endpoint class. """

from copy import deepcopy
from typing import Dict, Any
from unittest.mock import patch

import pytest

from verta.endpoint import Endpoint


@pytest.fixture
def mock_endpoint(mock_conn, mock_config) -> Endpoint:
    """ Return a mocked object of the Endpoint class for use in tests """
    return Endpoint(conn=mock_conn, conf=mock_config, workspace=456, id=123)

VERTA_CLASS: str= 'verta.endpoint.Endpoint'
WORKSPACE_ID: int = 456
DEPLOYMENT_ID: int = 123

# TODO: Generate responses dynamically from swagger to ensure ongoing consistency with back-end API schemas
# Expected response from the _get_json_by_id method as of 2022-10-24
GET_JSON_BY_ID_RESPONSE: Dict[str, Any] = {
    "creator_request": {
        "custom_permission": {
            "collaborator_type": "READ_ONLY"
        },
        "description": "test_description",
        "path": "/test_path",
        "resource_visibility": "PRIVATE",
        "visibility": "PRIVATE"
    },
    "date_created": "2022-10-20T00:00:00.000Z",
    "date_updated": "2022-10-20T00:00:00.000Z",
    "full_url": "https://test_socket/api/v1/predict/test_path",
    "id": DEPLOYMENT_ID,
    "owner_id": "test_owner",
    "workspace_id": WORKSPACE_ID
}

# Expected response from the get_status method as of 2022-10-24
GET_STATUS_RESPONSE: Dict[str, Any] = {
    "components": [
        {
            "build_id": 0,
            "message": "test_message",
            "ratio": 0,
            "status": "active"
        }
    ],
    "creator_request": {
        "autocreate_token": True,
        "enable_prediction_authz": False,
        "enable_prediction_tokens": True,
        "name": "test_name"
    },
    "date_created": "2022-10-20T00:00:00.000Z",
    "date_updated": "2022-10-20T00:00:00.000Z",
    "id": DEPLOYMENT_ID,
    "status": "active"
}

# Expected response from the get_access_token method as of 2022-10-24
GET_ACCESS_TOKEN_RESPONSE: str =  '123-test-456-token-789'

# TODO: Implement a less verbose, more universal method of patching method and http calls.
@patch(f'{VERTA_CLASS}.get_status', return_value=GET_STATUS_RESPONSE)
@patch(f'{VERTA_CLASS}._get_json_by_id', return_value=GET_JSON_BY_ID_RESPONSE)
@patch(f'{VERTA_CLASS}.get_access_token', return_value=GET_ACCESS_TOKEN_RESPONSE)
def test_get_deployed_model_call_get_status(mock_get_access_token,
                                            mock_get_json_by_id,
                                            mock_get_status,
                                            mock_endpoint,
                                            mock_conn) -> None:
    """ Verify that get_deployed_model calls the methods it should with the expected params """
    mock_endpoint.get_deployed_model()
    mock_get_status.assert_called_once()
    mock_get_json_by_id.assert_called_once_with(mock_conn, WORKSPACE_ID, DEPLOYMENT_ID)
    mock_get_access_token.assert_called_once()


@patch(f'{VERTA_CLASS}.get_status', return_value=GET_STATUS_RESPONSE)
@patch(f'{VERTA_CLASS}._get_json_by_id', return_value=GET_JSON_BY_ID_RESPONSE)
@patch(f'{VERTA_CLASS}.get_access_token', return_value=GET_ACCESS_TOKEN_RESPONSE)
def test_get_deployed_model_with_full_url(mock_get_access_token, # pass in patched methods in order
                                          mock_get_json_by_id,
                                          mock_get_status,
                                          mock_endpoint) -> None:
    """ Verify that the get_deployed_model method returns the correct value for the full_url when it is returned
       by the get_json_by_id method.  The 'full_url' key was added to the schema of the response 10/2022.
    """
    deployed_model = mock_endpoint.get_deployed_model()
    assert deployed_model.prediction_url == 'https://test_socket/api/v1/predict/test_path'


GET_JSON_BY_ID_RESPONSE_MISSING_URL: Dict[str, Any] = deepcopy(GET_JSON_BY_ID_RESPONSE)
GET_JSON_BY_ID_RESPONSE_MISSING_URL.pop('full_url')  # Remove the full url key and value from the dict.
@patch(f'{VERTA_CLASS}.get_status', return_value=GET_STATUS_RESPONSE)
@patch(f'{VERTA_CLASS}._get_json_by_id', return_value=GET_JSON_BY_ID_RESPONSE_MISSING_URL)
@patch(f'{VERTA_CLASS}.get_access_token', return_value=GET_ACCESS_TOKEN_RESPONSE)
def test_get_deployed_model_missing_full_url(mock_get_access_token, # pass in patched methods in order
                                             mock_get_json_by_id,
                                             mock_get_status,
                                             mock_endpoint) -> None:
    """ Verify that the get_deployed_model method returns the correct value for the full_url when it is returned
       by the get_json_by_id method.  The 'full_url' key was added to the schema of the response 10/2022.
    """
    deployed_model = mock_endpoint.get_deployed_model()
    assert deployed_model.prediction_url == 'https://test_socket/api/v1/predict/test_path'
