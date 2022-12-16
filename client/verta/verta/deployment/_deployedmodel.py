# -*- coding: utf-8 -*-

import json
import gzip
import os
import time
from urllib.parse import urlparse
import warnings

import requests

from ..external import six

from .._internal_utils import _utils
from verta import credentials
from .._internal_utils.access_token import AccessToken

# NOTE: DeployedModel's mechanism for making requests is independent from the
# rest of the client; Client's Connection deliberately instantiates a new
# Session for each request it makes otherwise it encounters de/serialization
# issues during parallelism, whereas DeployedModel persists a Session for its
# lifetime to use HTTP keep-alive and speed up consecutive predictions.


class DeployedModel(object):
    """
    Object for interacting with deployed models.

    This class provides functionality for sending predictions to a deployed model on the Verta
    backend.

    Authentication credentials will be picked up from environment variables if
    they are not supplied explicitly in the creds parameter.

    Parameters
    ----------
    prediction_url : str
        URL of the prediction endpoint
    token : str, optional
        Prediction token. Can be copy and pasted directly from the Verta Web App.
    creds : :class:`~verta.credentials.Credentials`, optional
        Authentication credentials to attach to each prediction request.

    Attributes
    ----------
    prediction_url : str
        Full prediction endpoint URL. Can be copy and pasted directly from the Verta Web App.
    access_token : str, optional
        Prediction token. Can be copy and pasted directly from the Verta Web App.
    credentials : :class:`~verta.credentials.Credentials`, optional
        Authentication credentials to attach to each prediction request.

    Examples
    --------
    .. code-block:: python

        DeployedModel(
            "https://app.verta.ai/api/v1/predict/01234567-0123-0123-0123-012345678901",
            token="abcdefgh-abcd-abcd-abcd-abcdefghijkl",
        )
        # <DeployedModel at https://app.verta.ai/api/v1/predict/01234567-0123-0123-0123-012345678901>

    """

    def __init__(self, prediction_url, token=None, creds=None):
        self.prediction_url = prediction_url
        self._credentials = creds or credentials.load_from_os_env()
        self._access_token = token
        self._session = None
        self._init_session()

    def _init_session(self):
        if self._session:
            self._session.close()
        session = requests.Session()
        if self.credentials:
            creds_headers = _utils.Connection.prefixed_headers_for_credentials(
                self.credentials
            )
            session.headers.update(creds_headers)
        if self.access_token:
            session.headers.update(AccessToken(self.access_token).headers())
        self._session = session

    def __repr__(self):
        return "<{} at {}>".format(self.__class__.__name__, self.prediction_url)

    @classmethod
    def from_url(cls, url, token=None, creds=None):
        """
        Returns a :class:`DeployedModel` based on a custom URL and token.

        Parameters
        ----------
        url : str
            Full prediction endpoint URL. Can be copy and pasted directly from the Verta Web App.
        token : str, optional
            Prediction token. Can be copy and pasted directly from the Verta Web App.
        creds : :class:`~verta.credentials.Credentials`, optional
            Authentication credentials to attach to each prediction request.

        Returns
        -------
        :class:`DeployedModel`

        Examples
        --------
        .. code-block:: python

            # run.id == "01234567-0123-0123-0123-012345678901"
            # token == "abcdefgh-abcd-abcd-abcd-abcdefghijkl"
            DeployedModel.from_url(
                url="https://app.verta.ai/api/v1/predict/01234567-0123-0123-0123-012345678901",
                token="abcdefgh-abcd-abcd-abcd-abcdefghijkl",
            )
            # <DeployedModel at https://app.verta.ai/api/v1/predict/01234567-0123-0123-0123-012345678901>

        """
        return cls(prediction_url=url, token=token, creds=creds)

    @property
    def prediction_url(self):
        return self._prediction_url

    @prediction_url.setter
    def prediction_url(self, value):
        parsed = urlparse(value)
        invalid = (
            not parsed or not parsed.scheme or not parsed.netloc or not parsed.path
        )
        if invalid:
            raise ValueError("not a valid prediction_url")
        self._prediction_url = parsed.geturl()

    @property
    def credentials(self):
        return self._credentials

    @credentials.setter
    def credentials(self, value):
        self._credentials = value
        self._init_session()

    @property
    def access_token(self):
        return self._access_token

    @access_token.setter
    def access_token(self, value):
        self._access_token = value
        self._init_session()

    def _predict(self, x, compress=False):
        """This is like ``DeployedModel.predict()``, but returns the raw ``Response`` for debugging."""
        x = _utils.to_builtin(x)
        if compress:
            # create gzip
            gzstream = six.BytesIO()
            with gzip.GzipFile(fileobj=gzstream, mode="wb") as gzf:
                gzf.write(six.ensure_binary(json.dumps(x)))
            gzstream.seek(0)

            return self._session.post(
                self._prediction_url,
                headers={"Content-Encoding": "gzip"},
                data=gzstream.read(),
            )
        else:
            return self._session.post(self.prediction_url, json=x)

    def headers(self):
        """Returns a copy of the headers attached to prediction requests."""
        return self._session.headers.copy()

    def get_curl(self):
        """
        Gets a valid cURL command.

        Returns
        -------
        str

        """
        headers = self.headers()
        headers.update({"Content-Type": "application/json"})
        curl = "curl -X POST {} -d '' -H \"Content-Type: application/json\"".format(
            self.prediction_url
        )
        for header, value in headers.items():
            curl += ' -H "{}: {}" '.format(header, value)
        return curl

    def predict(
        self,
        x,
        compress=False,
        max_retries=5,
        always_retry_404=True,
        always_retry_429=True,
    ):
        """
        Makes a prediction using input `x`.

        .. versionadded:: 0.13.14
           The `always_retry_404` parameter.
        .. versionadded:: 0.13.12
           The `always_retry_429` parameter.

        Parameters
        ----------
        x : list
            A batch of inputs for the model.
        compress : bool, default False
            Whether to compress the request body.
        max_retries : int, default 5
            Maximum number of times to retry a request on a connection failure.
        always_retry_404 : bool, default True
            Whether to retry on 404s indefinitely. This is to accommodate model deployment warm-up.
        always_retry_429 : bool, default True
            Whether to retry on 429s indefinitely. This is to accommodate third-party cluster
            autoscalers, which may take minutes to launch new pods for servicing requests.

        Returns
        -------
        prediction : list
            Output returned by the deployed model for `x`.

        Raises
        ------
        RuntimeError
            If the deployed model encounters an error while running the prediction.
        requests.HTTPError
            If the server encounters an error while handing the HTTP request.

        """
        num_retries = 0
        while num_retries < max_retries:
            response = self._predict(x, compress)

            if response.ok:
                return _utils.body_to_json(response)
            elif response.status_code in (
                400,
                502,
            ):  # possibly error from the model back end
                try:
                    data = _utils.body_to_json(response)
                except ValueError:  # not JSON response; 502 not from model back end
                    pass
                else:  # from model back end; contains message (maybe)
                    # try to directly print message, otherwise line breaks appear as '\n'
                    msg = data.get("message") or json.dumps(data)
                    raise RuntimeError(
                        "deployed model encountered an error: {}".format(msg)
                    )
            elif not (
                response.status_code >= 500 or response.status_code in (404, 429)
            ):  # clientside error
                break

            sleep = 0.3 * (2 ** (num_retries + 1))
            print(
                "received status {}; retrying in {:.1f}s".format(
                    response.status_code, sleep
                )
            )
            time.sleep(sleep)
            if (response.status_code == 404 and always_retry_404) or (  # model warm-up
                response.status_code == 429 and always_retry_429
            ):  # too many requests
                num_retries = min(num_retries + 1, max_retries - 1)
            else:
                num_retries += 1

        _utils.raise_for_http_error(response)

    def predict_with_id(
        self,
        x,
        compress=False,
        max_retries=5,
        always_retry_404=True,
        always_retry_429=True,
    ):
        num_retries = 0
        while num_retries < max_retries:
            response = self._predict(x, compress)

            if response.ok:
                # This is the only part that changes
                data = _utils.body_to_json(response)
                headers = response.headers
                id = headers["verta-request-id"]  # TODO: accept ID from the user
                return id, data
            elif response.status_code in (
                400,
                502,
            ):  # possibly error from the model back end
                try:
                    data = _utils.body_to_json(response)
                except ValueError:  # not JSON response; 502 not from model back end
                    pass
                else:  # from model back end; contains message (maybe)
                    # try to directly print message, otherwise line breaks appear as '\n'
                    msg = data.get("message") or json.dumps(data)
                    raise RuntimeError(
                        "deployed model encountered an error: {}".format(msg)
                    )
            elif not (
                response.status_code >= 500 or response.status_code in (404, 429)
            ):  # clientside error
                break

            sleep = 0.3 * (2 ** (num_retries + 1))
            print(
                "received status {}; retrying in {:.1f}s".format(
                    response.status_code, sleep
                )
            )
            time.sleep(sleep)
            if (response.status_code == 404 and always_retry_404) or (  # model warm-up
                response.status_code == 429 and always_retry_429
            ):  # too many requests
                num_retries = min(num_retries + 1, max_retries - 1)
            else:
                num_retries += 1

        _utils.raise_for_http_error(response)


def prediction_input_unpack(func):
    """
    Decorator for unpacking a dictionary passed in as the argument for ``predict()``.

    Verta's :meth:`DeployedModel.predict` does not support passing multiple arguments to the model.
    If an existing model of yours was written to take multiple parameters, this decorator can bridge
    the gap to keep your model code clean by allowing you to pass in a dictionary of keyword arguments
    and values that will be unpacked for ``predict()``.

    .. versionadded:: 0.13.18

    Examples
    --------
    Before::

        class Model(object):
            def predict(self, data):
                x = data['x']
                y = data['y']
                z = data['z']
                return x + y + z

        deployed_model.predict({'x': 0, 'y': 1, 'z': 2})
        # 3

    After::

        class Model(object):
            @prediction_input_unpack
            def predict(self, x, y, z):
                return x + y + z

        deployed_model.predict({'x': 0, 'y': 1, 'z': 2})
        # 3

    """

    def prediction(self, X):
        return func(self, **X)

    return prediction


def prediction_io_cleanup(func):
    """
    Decorator for casting the argument and return values for ``predict()`` into Python built-in types.

    For interoperability, a deployed model will receive and return Python's built-in types—such as
    lists rather than NumPy arrays. There may be inconsistencies as you develop your model locally
    if your ``predict()`` code is written to expect and/or output a third-party type; this decorator
    will attempt to cast such values into a Python built-in type, replicating
    :meth:`DeployedModel.predict`'s behavior.

    .. versionadded:: 0.13.17

    Examples
    --------
    Before::

        class Model(object):
            def predict(self, data):
                return data.mean()

        data = np.array([0, 1, 2])
        model.predict(data)  # succeeds; predict() locally receives NumPy array
        # 1.0
        deployed_model.predict(data)  # fails; predict() in deployment receives list
        # HTTPError: 400 Client Error: Traceback (most recent call last):
        #   File "<stdin>", line 3, in predict
        # AttributeError: 'list' object has no attribute 'mean'
        #  for url: https://app.verta.ai/api/v1/predict/01234567-0123-0123-0123-012345678901

    After::

        class Model(object):
            @prediction_io_cleanup
            def predict(self, data):
                # anticipate `data` being list
                return sum(data) / float(len(data))

        data = np.array([1, 2, 3])
        # consistent behavior locally and in deployment
        model.predict(data)
        # 1.0
        deployed_model.predict(data)
        # 1.0

    """

    def prediction(self, X):
        return _utils.to_builtin(func(self, _utils.to_builtin(X)))

    return prediction
