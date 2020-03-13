import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from ..._utils import _VALID_HTTP_METHODS, raise_for_http_error
from ..base_type import BaseType
import json

class HttpClient(object):
    def __init__(self, host, headers, retries=3, ignore_conn_err=False):
        self.host = host
        self.headers = headers
        self.retries = retries
        self.ignore_conn_err = ignore_conn_err

    def request(self, method, path, query, body):
        if method.upper() not in _VALID_HTTP_METHODS:
            raise ValueError("`method` must be one of {}".format(_VALID_HTTP_METHODS))

        url = self.host + path
        if isinstance(body, BaseType):
            body = json.dumps(body.to_json())

        with requests.Session() as s:

            s.mount(url, HTTPAdapter(max_retries=self.retries))
            try:
                response = s.request(method, url, headers=self.headers, data=body)
            except (requests.exceptions.BaseHTTPError,
                    requests.exceptions.RequestException) as e:
                if not self.ignore_conn_err:
                    raise e
            else:
                raise_for_http_error(response)
                if response.ok:
                    return response.json()
            return None

    # TODO
    def to_query(self, value):
        return value
