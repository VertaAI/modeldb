# -*- coding: utf-8 -*-
""" Unit tests for the verta.deployment.DeployedModel class. """
import os
from typing import Any, Dict

import pytest
from requests import Session, HTTPError
from requests.exceptions import RetryError
import responses
from unittest.mock import patch
from urllib3 import Retry

from verta.credentials import EmailCredentials
from verta.deployment import DeployedModel
from verta._internal_utils import http_session

PREDICTION_URL: str = 'https://test.dev.verta.ai/api/v1/predict/test_path'
TOKEN: str = '12345678-xxxx-1a2b-3c4d-e5f6g7h8'
MOCK_RETRY: Retry = http_session.retry_config(
    max_retries=http_session.DEFAULT_MAX_RETRIES,
    status_forcelist=http_session.DEFAULT_STATUS_FORCELIST,
    backoff_factor=http_session.DEFAULT_BACKOFF_FACTOR
    )
MOCK_SESSION: Session = http_session.init_session(retry=MOCK_RETRY)
VERTA_CLASS = 'verta.deployment._deployedmodel'

@pytest.fixture
def mocked_responses():
    with responses.RequestsMock() as rsps:
        yield rsps


@patch.dict(
    os.environ,
    {'VERTA_EMAIL': 'test_email@verta.ai',
     'VERTA_DEV_KEY':'123test1232dev1232key123'},
    )
@patch(
    f'{VERTA_CLASS}.http_session.retry_config',
    return_value=MOCK_RETRY,
    )
@patch(
    f'{VERTA_CLASS}.http_session.init_session',
    return_value=MOCK_SESSION,
    )
def test_deployed_model_init(mock_session, mock_retry) -> None:
    """ Validate the creation of an object of deployment.DeployedModel class with desired Session. """
    creds = EmailCredentials.load_from_os_env()
    created_dm_details = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        ).__dict__
    expected_dm_details: Dict[str, Any] = {
        '_prediction_url': PREDICTION_URL,
        '_credentials': creds,
        '_access_token': '12345678-xxxx-1a2b-3c4d-e5f6g7h8',
        '_retry_config': mock_retry.return_value,
        '_session': mock_session.return_value
        }
    assert created_dm_details['_prediction_url'] == expected_dm_details['_prediction_url']
    assert created_dm_details['_access_token'] == expected_dm_details['_access_token']
    assert created_dm_details['_credentials'] == expected_dm_details['_credentials']
    assert created_dm_details['_session'] == expected_dm_details['_session']


def test_predict_http_defaults_200(mocked_responses) -> None:
    """ Calling predict with the default settings and getting a 200 response returns the response as expected. """
    mocked_responses.post(
        PREDICTION_URL,
        json={"test_key": "test_val"},
        status=200,
        headers={'verta-request-id': 'hereISaTESTidFROMtheUSER'},
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    prediction_response = dm.predict(x=['test_prediction'])
    assert prediction_response == {"test_key": "test_val"}


def test_predict_http_defaults_404_retry_error(mocked_responses) -> None:
    """ Calling predict with the default settings and getting a 404 results in retries being exhausted. """
    mocked_responses.post(
        PREDICTION_URL,
        json={},
        status=404,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    with pytest.raises(RetryError):
        dm.predict(x=['test_prediction'])


def test_predict_http_defaults_429_retry_error(mocked_responses) -> None:
    """ Calling predict with the default settings and getting a 429 results in retries being exhausted. """
    mocked_responses.post(
        PREDICTION_URL,
        json={},
        status=429,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    with pytest.raises(RetryError):
        dm.predict(x=['test_prediction'])


def test_predict_http_defaults_status_not_in_retry(mocked_responses) -> None:
    """ Verify that calling predict with the default settings and getting a response not in `status_forcelist`
     does not result in retries. """
    mocked_responses.post(
        PREDICTION_URL,
        headers={'verta-request-id': 'hereISaTESTidFROMtheUSER'},
        json={},
        status=999,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    dm.predict(x=['test_prediction'])
    mocked_responses.assert_call_count(PREDICTION_URL, 1)


def test_predict_http_default_max_retry_observed(mocked_responses) -> None:
    """ Calling predict with the default settings and getting a 429 results in retries being exhausted. """
    mocked_responses.post(
        PREDICTION_URL,
        json={},
        status=429,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    with pytest.raises(RetryError):
        dm.predict(x=['test_prediction'])
    mocked_responses.assert_call_count(PREDICTION_URL, http_session.DEFAULT_MAX_RETRIES + 1)
    # max_retries + 1 original attempt = total call count


def test_predict_with_altered_retry_config(mocked_responses) -> None:
    """ Calling predict with custom retry parameters changes the retry config and makes the correct requests. """
    mocked_responses.post(
        PREDICTION_URL,
        json={},
        status=888,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    with pytest.raises(RetryError):
        dm.predict(
            x=['test_prediction'],
            max_retries=9,
            retry_status={888},
            backoff_factor=0.1
            )
    mocked_responses.assert_call_count(PREDICTION_URL, 10)


def test_predict_with_prediction_id_provided(mocked_responses) -> None:
    """ Calling predict while providing a value for `prediction_id` updates and includes the headers in the request. """
    mocked_responses.post(
        PREDICTION_URL,
        json={'test1': 'test1'},
        status=200,
        match=[responses.matchers.header_matcher({'verta-request-id': 'hereISaTESTidFROMtheUSER'})],
        headers={'verta-request-id': 'hereISaTESTidFROMtheUSER'},
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    dm.predict(
        x=['test_prediction'],
        prediction_id='hereISaTESTidFROMtheUSER',
        )
    mocked_responses.assert_call_count(PREDICTION_URL, 1)


def test_predict_with_id_response_includes_id(mocked_responses) -> None:
    """ Calling predict_with_id returns both the ID from teh request response, and the prediction results """
    mocked_responses.post(
        PREDICTION_URL,
        headers={'verta-request-id': 'AutoGeneratedTestId'},
        # Adds this header to the mocked http response.
        json={'test2': 'test2'},
        status=200,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    prediction = dm.predict_with_id(x=['test_prediction'])
    assert prediction == ('AutoGeneratedTestId', {'test2': 'test2'})


def test_predict_with_id_prediction_id_provided(mocked_responses) -> None:
    """ Calling predict_with_id while including the  `prediction_id` adds the id to the header of the request and
      includes the id provided in the response with the prediction results """
    mocked_responses.post(
        PREDICTION_URL,
        match=[responses.matchers.header_matcher({'verta-request-id': 'hereISomeTESTidFROMtheUSER'})],
        # Makes sure the prediction id was included as a header in the request
        headers={'verta-request-id': 'hereISomeTESTidFROMtheUSER'},
        # Adds this header to the mocked http response.
        json={'test2': 'test2'},
        status=200,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    prediction = dm.predict_with_id(
        x=['test_prediction'],
        prediction_id='hereISomeTESTidFROMtheUSER'
        )
    assert prediction == ('hereISomeTESTidFROMtheUSER', {'test2': 'test2'})


def test_predict_with_id_http_defaults_200(mocked_responses) -> None:
    """ Calling predict with the default settings and getting a 200 response returns the response as expected. """
    mocked_responses.post(
        PREDICTION_URL,
        json={"test_key": "test_val"},
        status=200,
        headers={'verta-request-id': 'hereISthisTESTidFROMtheUSER'},
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    prediction_response = dm.predict_with_id(x=['test_prediction'])
    assert prediction_response == ('hereISthisTESTidFROMtheUSER', {"test_key": "test_val"})


def test_predict_with_id_http_defaults_404_retry_error(mocked_responses) -> None:
    """ Calling predict with the default settings and getting a 404 results in retries being exhausted. """
    mocked_responses.post(
        PREDICTION_URL,
        json={},
        status=404,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    with pytest.raises(RetryError):
        dm.predict_with_id(x=['test_prediction'])


def test_predict_with_id_altered_retry_config(mocked_responses) -> None:
    """ Calling predict with custom retry parameters changes the retry config and makes the correct requests. """
    mocked_responses.post(
        PREDICTION_URL,
        json={},
        status=888,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    with pytest.raises(RetryError):
        dm.predict_with_id(
            x=['test_prediction'],
            max_retries=9,
            retry_status={888},
            backoff_factor=0.1
            )
    mocked_responses.assert_call_count(PREDICTION_URL, 10)


def test_default_retry_after_custom_retry(mocked_responses) -> None:
    """ Calling predict with default params after calling predict with custom
        params uses default retry settings and not the custom settings from
        the previous call. """
    mocked_responses.post(
        PREDICTION_URL,
        json={},
        status=777,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
        )
    with pytest.raises(RetryError):
        dm.predict(
            x=['test_prediction'],
            max_retries=1,
            retry_status={777},
            backoff_factor=0.1,
            )
    mocked_responses.assert_call_count(PREDICTION_URL, 2)
    # 1 attempt + 1 retry = 2

    mocked_responses.post(
        PREDICTION_URL,
        json={},
        status=429,
        )
    with pytest.raises(RetryError):
        dm.predict(x=['test_prediction'])  # use defaults
    mocked_responses.assert_call_count(PREDICTION_URL, 16)
    # previous 2 + 1 attempt + default 13 retries = 16


def test_predict_400_error_message_extraction(mocked_responses) -> None:
    """ Getting a 400 will render the attached message form the backend if present """
    mocked_responses.post(
        PREDICTION_URL,
        json={"message": "Here be a message in the response"},
        status=400,
        headers={'verta-request-id': 'AutoGeneratedTestId'},
    )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
    )
    with pytest.raises(RuntimeError) as err:
        dm.predict(x=['test_prediction'])
    assert str(err.value) == (
        'deployed model encountered an error: Here be a message in the response'
        )


def test_predict_400_error_message_missing(mocked_responses) -> None:
    """ Getting a 401 error, with no message provided by the back-end will fall back
        to raise_for_http_error style error formatting.
     """
    mocked_responses.post(
        PREDICTION_URL,
        status=400,
        headers={'verta-request-id': 'AutoGeneratedTestId'},
    )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
    )
    with pytest.raises(HTTPError) as err:
        dm.predict(x=['test_prediction'])
    assert str(err.value)[:-30] == (
        '400 Client Error: Bad Request for url: '
        'https://test.dev.verta.ai/api/v1/predict/test_path at '
    )
