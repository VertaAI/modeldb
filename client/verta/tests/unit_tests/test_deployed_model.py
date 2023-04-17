# -*- coding: utf-8 -*-
""" Unit tests for the verta.deployment.DeployedModel class. """
import json
import os
import random
from typing import Any, Dict

import hypothesis
import pytest

from tests import utils

np = pytest.importorskip("numpy")
pd = pytest.importorskip("pandas")
from requests import Session, HTTPError
from requests.exceptions import RetryError
import responses
from unittest.mock import patch
from urllib3 import Retry
from hypothesis import strategies as st
from hypothesis import given

from verta.credentials import EmailCredentials
from verta.deployment import DeployedModel
from verta._internal_utils import http_session

PREDICTION_URL: str = 'https://test.dev.verta.ai/api/v1/predict/test_path'
BATCH_PREDICTION_URL: str = 'https://test.dev.verta.ai/api/v1/batch-predict/test_path'
TOKEN: str = '12345678-xxxx-1a2b-3c4d-e5f6g7h8'
MOCK_RETRY: Retry = http_session.retry_config(
    max_retries=http_session.DEFAULT_MAX_RETRIES,
    status_forcelist=http_session.DEFAULT_STATUS_FORCELIST,
    backoff_factor=http_session.DEFAULT_BACKOFF_FACTOR
)
MOCK_SESSION: Session = http_session.init_session(retry=MOCK_RETRY)
VERTA_CLASS = 'verta.deployment._deployedmodel'


@patch.dict(
    os.environ,
    {'VERTA_EMAIL': 'test_email@verta.ai',
     'VERTA_DEV_KEY': '123test1232dev1232key123'},
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


def test_batch_predict_with_one_batch_with_no_index(mocked_responses) -> None:
    """ Call batch_predict with a single batch. """
    expected_df = pd.DataFrame({"A": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10], "B": [11, 12, 13, 14, 15, 16, 17, 18, 19, 20]})
    expected_df_body = json.dumps(expected_df.to_dict(orient="split"))
    mocked_responses.post(
        BATCH_PREDICTION_URL,
        body=expected_df_body,
        status=200,
    )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
    )
    # the input below is entirely irrelevant since it's smaller than the batch size
    prediction_df = dm.batch_predict(pd.DataFrame({"hi": "bye"}, index=[1]), 10)
    pd.testing.assert_frame_equal(expected_df, prediction_df)


def test_batch_predict_with_one_batch_with_index(mocked_responses) -> None:
    """ Call batch_predict with a single batch, where the output has an index. """
    expected_df = pd.DataFrame({"A": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10], "B": [11, 12, 13, 14, 15, 16, 17, 18, 19, 20]},
                               index=["a", "b", "c", "d", "e", "f", "g", "h", "i", "j"])
    expected_df_body = json.dumps(expected_df.to_dict(orient="split"))
    mocked_responses.post(
        BATCH_PREDICTION_URL,
        body=expected_df_body,
        status=200,
    )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
    )
    # the input below is entirely irrelevant since it's smaller than the batch size
    prediction_df = dm.batch_predict(pd.DataFrame({"hi": "bye"}, index=[1]), 10)
    pd.testing.assert_frame_equal(expected_df, prediction_df)


def test_batch_predict_with_five_batches_with_no_indexes(mocked_responses) -> None:
    """ Since the input has 5 rows and we're providing a batch_size of 1, we expect 5 batches."""
    expected_df_list = [pd.DataFrame({"A": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}),
                        pd.DataFrame({"B": [11, 12, 13, 14, 15, 16, 17, 18, 19, 20]}),
                        pd.DataFrame({"C": [21, 22, 23, 24, 25, 26, 27, 28, 29, 30]}),
                        pd.DataFrame({"D": [31, 32, 33, 34, 35, 36, 37, 38, 39, 40]}),
                        pd.DataFrame({"E": [41, 42, 43, 44, 45, 46, 47, 48, 49, 50]}),
                        ]
    for expected_df in expected_df_list:
        mocked_responses.add(
            responses.POST,
            BATCH_PREDICTION_URL,
            body=json.dumps(expected_df.to_dict(orient="split")),
            status=200,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
    )
    input_df = pd.DataFrame({"a": [1, 2, 3, 4, 5], "b": [11, 12, 13, 14, 15]})
    prediction_df = dm.batch_predict(input_df, batch_size=1)
    expected_df = pd.concat(expected_df_list)
    pd.testing.assert_frame_equal(expected_df, prediction_df)


def test_batch_predict_with_batches_and_indexes(mocked_responses) -> None:
    """ Since the input has 5 rows and we're providing a batch_size of 1, we expect 5 batches.
    Include an example of an index.
    """
    expected_df_list = [
        pd.DataFrame({"A": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}, index=["a", "b", "c", "d", "e", "f", "g", "h", "i", "j"]),
        pd.DataFrame({"B": [11, 12, 13, 14, 15, 16, 17, 18, 19, 20]},
                     index=["a", "b", "c", "d", "e", "f", "g", "h", "i", "j"]),
        pd.DataFrame({"C": [21, 22, 23, 24, 25, 26, 27, 28, 29, 30]},
                     index=["a", "b", "c", "d", "e", "f", "g", "h", "i", "j"]),
        pd.DataFrame({"D": [31, 32, 33, 34, 35, 36, 37, 38, 39, 40]},
                     index=["a", "b", "c", "d", "e", "f", "g", "h", "i", "j"]),
        pd.DataFrame({"E": [41, 42, 43, 44, 45, 46, 47, 48, 49, 50]},
                     index=["a", "b", "c", "d", "e", "f", "g", "h", "i", "j"]),
        ]
    for expected_df in expected_df_list:
        mocked_responses.add(
            responses.POST,
            BATCH_PREDICTION_URL,
            body=json.dumps(expected_df.to_dict(orient="split")),
            status=200,
        )
    creds = EmailCredentials.load_from_os_env()
    dm = DeployedModel(
        prediction_url=PREDICTION_URL,
        creds=creds,
        token=TOKEN,
    )
    input_df = pd.DataFrame({"a": [1, 2, 3, 4, 5], "b": [11, 12, 13, 14, 15]}, index=["A", "B", "C", "D", "E"])
    prediction_df = dm.batch_predict(input_df, 1)
    expected_final_df = pd.concat(expected_df_list)
    pd.testing.assert_frame_equal(expected_final_df, prediction_df)


@st.composite
def generate_data(draw, max_rows=50, max_cols=6):
    """ Return a dict that represents a dataframe. Generates ints, floats, and strings."""
    num_rows = draw(st.integers(min_value=1, max_value=max_rows))
    num_cols = draw(st.integers(min_value=1, max_value=max_cols))
    col_names = draw(st.lists(st.text(), max_size=num_cols, min_size=num_cols, unique=True))
    data = {}
    for name in col_names:

        type_probability = utils.gen_probability()
        if type_probability <= 0.3:
            col_values = st.integers()
        elif type_probability <= 0.6:
            col_values = st.floats()
        else:
            col_values = st.text()
        col = draw(st.lists(col_values, max_size=num_rows, min_size=num_rows))
        data[name] = col

    out_dict = {"data": data}
    index_probability = utils.gen_probability()
    if index_probability <= 0.5:
        index = draw(st.lists(st.text(), max_size=num_rows, min_size=num_rows))
        out_dict["index"] = index
    return out_dict


@hypothesis.settings(deadline=None)  # client utils make DataFrame handling slow at first
@given(json_df=generate_data(), batch_size=st.integers(min_value=1, max_value=10))
def test_batch(json_df, batch_size) -> None:
    """ Test that the batch_predict method works with a variety of inputs. """
    with responses.RequestsMock() as rsps:
        if "index" in json_df:
            input_df = pd.DataFrame(json_df["data"], index=json_df["index"])
        else:
            input_df = pd.DataFrame(json_df["data"])
        for i in range(0, len(input_df), batch_size):
            batch = input_df.iloc[i:i + batch_size]
            serialized_batch = batch.to_dict(orient="split")
            rsps.add(
                responses.POST,
                BATCH_PREDICTION_URL,
                body=json.dumps(serialized_batch),
                status=200,
            )
        creds = EmailCredentials.load_from_os_env()
        dm = DeployedModel(
            prediction_url=PREDICTION_URL,
            creds=creds,
            token=TOKEN,
        )
        prediction_df = dm.batch_predict(input_df, batch_size=batch_size)
        pd.testing.assert_frame_equal(input_df, prediction_df)

