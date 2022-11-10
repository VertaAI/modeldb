# -*- coding: utf-8 -*-
""" Unit tests for the request session utilities found in verta._internal_utils.http_session """

import pytest
import requests

from requests import Session
from requests.adapters import HTTPAdapter
from requests.cookies import RequestsCookieJar
from typing import OrderedDict
from unittest.mock import patch
from urllib3 import Retry

from verta._internal_utils import http_session

# Pytest fixtures cannot be passed to the @patch decorator
# as return values, hence the use of global vars instead.
# Retry object for patching
MOCK_RETRY_OBJECT: Retry = http_session.retry_config(
    max_retries=11,
    status_forcelist={123, 456},
    backoff_factor=1.23
)

# A different retry object for testing on-the-fly alteration
ALTERED_MOCK_RETRY_OBJECT: Retry = http_session.retry_config(
    max_retries=22,
    status_forcelist={789},
    backoff_factor=2.45
)
# HTTPAdapter object for patching
MOCK_HTTP_ADAPTER = HTTPAdapter(max_retries=MOCK_RETRY_OBJECT)


def test_retry_config() -> None:
    """ Verify that our use of the Retry object returns the object as expected """
    mock_retry_object = MOCK_RETRY_OBJECT
    assert mock_retry_object.backoff_factor == 1.23
    assert mock_retry_object.status_forcelist == {123, 456}
    assert mock_retry_object.status == 11
    assert mock_retry_object.total == None
    assert mock_retry_object.allowed_methods == False
    assert mock_retry_object.connect == 0
    assert mock_retry_object.other == 0
    assert mock_retry_object.raise_on_status == True
    assert mock_retry_object.raise_on_redirect == True


def test_set_retry_config() -> None:
    """  The set_retry_config method correctly updates the retry config of the
     session object """
    sesh = http_session.init_session(MOCK_RETRY_OBJECT)
    http_session.set_retry_config(
        sesh,
        max_retries=22,
        status_forcelist={789},
        backoff_factor=2.45,
    )
    https_retry_object = sesh.get_adapter('https://').max_retries
    assert https_retry_object.backoff_factor == 2.45
    assert https_retry_object.status_forcelist == {789}
    assert https_retry_object.status == 22
    http_retry_object = sesh.get_adapter('http://').max_retries
    assert http_retry_object.backoff_factor == 2.45
    assert http_retry_object.status_forcelist == {789}
    assert http_retry_object.status == 22


@patch.object(http_session, 'HTTPAdapter', return_value=MOCK_HTTP_ADAPTER)
def test_init_session(mock_adapter) -> None:
    """ Verify that the requests.Session object is returned with the correct
    configuration given the inputs """
    expected_session_config = {
        'headers': {'User-Agent': f'python-requests/{requests.__version__}',
                    'Accept-Encoding': 'gzip, deflate',
                    'Accept': '*/*',
                    'Connection': 'keep-alive'},
        'auth': None,
        'proxies': {},
        'hooks': {
            'response': []
        },
        'params': {},
        'stream': False,
        'verify': True,
        'cert': None,
        'max_redirects': 30,
        'trust_env': True,
        'cookies': RequestsCookieJar(),
        'adapters': OrderedDict([
            ('https://', mock_adapter.return_value),
            ('http://', mock_adapter.return_value)
        ])
    }
    created_session = http_session.init_session(MOCK_RETRY_OBJECT)
    assert created_session.__dict__ == expected_session_config
    assert created_session.get_adapter('https://').max_retries == MOCK_RETRY_OBJECT
    assert created_session.get_adapter('http://').max_retries == MOCK_RETRY_OBJECT
