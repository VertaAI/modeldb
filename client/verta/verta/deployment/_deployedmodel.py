# -*- coding: utf-8 -*-

import gzip
import json
import warnings

from requests import Session
from typing import Any, Dict, List, Optional, Set, Tuple
from urllib.parse import urlparse
from verta import credentials

from .._internal_utils import _utils, http_session
from .._internal_utils._utils import Connection
from .._internal_utils.access_token import AccessToken
from ..external import six

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
       :emphasize-lines: 1,3

        # Preferred method for instantiating an object.
        endpoint = client.get_or_create_endpoint('endpoint_name')
        deployed_model = endpoint.get_deployed_model()
        deployed_model.predict(['here is a prediction'])

        # Instantiating directly is also possible.
        DeployedModel(
            "https://app.verta.ai/api/v1/predict/01234567-0123-0123-0123-012345678901",
            token="abcdefgh-abcd-abcd-abcd-abcdefghijkl",
        )
        # <DeployedModel at https://app.verta.ai/api/v1/predict/01234567-0123-0123-0123-012345678901>

    """

    def __init__(
            self,
            prediction_url,
            token=None,
            creds=None
            ):
        self.prediction_url: str = prediction_url
        self._credentials: credentials.Credentials = creds or credentials.load_from_os_env()
        self._access_token: str = token
        self._session: Session = None
        self._init_session()

    def _init_session(self):
        """ Create a single persistent Session object. """
        session: Session = http_session.init_session(retry=http_session.retry_config())

        if self._credentials:
            session.headers.update(
                Connection.prefixed_headers_for_credentials(self._credentials)
                )
        if self._access_token:
            session.headers.update(
                AccessToken(self._access_token).headers()
                )
        self._session = session

    def __repr__(self):
        return f"<{self.__class__.__name__} at {self.prediction_url}>"

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


    # TODO: Implement dynamic compression via separate utility and call it from here
    def _predict(
            self,
            x: Any,
            compress: bool=False,
            prediction_id: Optional[str]=None,
            ):
        """This is like ``DeployedModel.predict()``, but returns the raw ``Response``
         for debugging."""
        request_headers = dict()
        if prediction_id:
            request_headers.update({'verta-request-id': prediction_id})

        x = _utils.to_builtin(x)
        if compress:
            request_headers.update({'Content-Encoding': 'gzip'})
            # create gzip
            gzstream = six.BytesIO()
            with gzip.GzipFile(fileobj=gzstream, mode="wb") as gzf:
                gzf.write(six.ensure_binary(json.dumps(x)))
            gzstream.seek(0)

            response = self._session.post(
                self._prediction_url,
                headers=request_headers,
                data=gzstream.read(),
                )
        else:
            response = self._session.post(
                self.prediction_url,
                headers=request_headers,
                json=x,
                )
        if response.status_code in (
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
                raise RuntimeError(f"deployed model encountered an error: {msg}")

        if not response.ok:
            _utils.raise_for_http_error(response=response)
        return response


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
        curl = f"curl -X POST {self.prediction_url} -d '' -H \"Content-Type: application/json\""
        for header, value in headers.items():
            curl += f" -H \"{header}: {value}\""
        return curl

    # TODO: Removed deprecated `always_retry_404` and `always_retry_429` params
    def predict(
        self,
        x: List[Any],
        compress=False,
        max_retries: int = http_session.DEFAULT_MAX_RETRIES,
        always_retry_404=False,
        always_retry_429=False,
        retry_status: Set[int] = http_session.DEFAULT_STATUS_FORCELIST,
        backoff_factor: float = http_session.DEFAULT_BACKOFF_FACTOR,
        prediction_id: str = None,
        ) -> Dict[str, Any]:
        """
        Makes a prediction using input `x`.

        .. deprecated:: 0.22.0
           The ``always_retry_404`` parameter is deprecated. 404 is included by default in the
           ``retry_status`` parameter.  Default is 13 retries over 10 minutes.  This behavior
           can be changed by adjusting ``max_retries`` and ``backoff_factor``.
        .. deprecated:: 0.22.0
           The ``always_retry_429`` parameter is deprecated. 429 is included by default in the
           ``retry_status`` parameter.  Default is 13 retries over 10 minutes.  This behavior
           can be changed by adjusting ``max_retries`` and ``backoff_factor``.
        .. versionadded:: 0.22.0
           The ``retry_status`` parameter.
        .. versionadded:: 0.22.0
           The ``backoff_factor`` parameter.
        .. versionadded:: 0.22.0
           The ``prediction_id`` parameter.

        Parameters
        ----------
        x : list
            A batch of inputs for the model.
        compress : bool, default False
            Whether to compress the request body.
        max_retries : int, default 13
            Maximum number of retries on status codes listed in ``retry_status``.
        always_retry_404 : bool, default False
            Deprecated: Whether to retry on 404s indefinitely. This is to accommodate model deployment warm-up.
        always_retry_429 : bool, default False
            Deprecated: Whether to retry on 429s indefinitely. This is to accommodate third-party cluster
            autoscalers, which may take minutes to launch new pods for servicing requests.
        retry_status : set, default {404, 429, 500, 503, 504}
            Set of status codes, as integers, for which retry attempts should be made.  Overwrites default value.
            Expand the set to include more. For example, to add status code 409 to the existing set, use:
            ``retry_status={404, 429, 500, 503, 504, 409}``
        backoff_factor : float, default 0.3
            A backoff factor to apply between retry attempts.  Uses standard urllib3 sleep pattern:
            ``{backoff factor} * (2 ** ({number of total retries} - 1))`` with a maximum sleep time between requests of
            120 seconds.
        prediction_id: str, default None
            A custom str to use as the ID for the prediction request.  Defaults to a randomly generated numeric id.
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
        if always_retry_404 or always_retry_429:
            warnings.warn(
                "Deprecation warning: The `always_retry_404` and `always_retry_429`"
                "parameters are deprecated and will removed in an upcoming version; "
                "Status codes that should be retried are passed in the `retry_status`"
                "parameter which includes 404 and 429 by default.",
                category=FutureWarning,
            )
        prediction_with_id: Tuple[str, Any] = self.predict_with_id(
            x=x,
            compress=compress,
            prediction_id=prediction_id,
            max_retries=max_retries,
            retry_status=retry_status,
            backoff_factor=backoff_factor,
            )
        return prediction_with_id[1]


    def predict_with_id(
            self,
            x: List[Any],
            compress=False,
            max_retries: int = http_session.DEFAULT_MAX_RETRIES,
            retry_status: Set[int] = http_session.DEFAULT_STATUS_FORCELIST,
            backoff_factor: float = http_session.DEFAULT_BACKOFF_FACTOR,
            prediction_id: str = None,
            ) -> Tuple[str, List[Any]]:
        """
        Makes a prediction using input `x` the same as `predict`, but returns a tuple including the ID of the
        prediction request along with the prediction results.

        .. versionadded:: 0.22.0
           The `prediction_id` parameter.
        .. versionadded:: 0.22.0
           The `retry_status` parameter.
        .. versionadded:: 0.22.0
           The `backoff_factor` parameter.

        Parameters
        ----------
        x : list
            A batch of inputs for the model.
        compress : bool, default False
            Whether to compress the request body.
        max_retries : int, default 13
            Maximum number of retries on status codes listed in ``retry_status`` parameter only.
        retry_status : set, default {404, 429, 500, 503, 504}
            Set of status codes, as integers, for which retry attempts should be made.  Overwrites default value.
            Expand the set to include more. For example, to add status code 409 to the existing set, use:
            ``retry_status={404, 429, 500, 503, 504, 409}``
        backoff_factor : float, default 0.3
            A backoff factor to apply between retry attempts.  Uses standard urllib3 sleep pattern:
            ``{backoff factor} * (2 ** ({number of total retries} - 1))`` with a maximum sleep time between requests of
            120 seconds.
        prediction_id: str, optional
            A custom str to use as the ID for the prediction request.  Defaults to a randomly generated numeric id.
        Returns
        -------
        id : str
            The prediction ID.
        prediction: List[Any]
            The output returned by the deployed model for `x`.

        Raises
        ------
        RuntimeError
            If the deployed model encounters an error while running the prediction.
        requests.HTTPError
            If the server encounters an error while handing the HTTP request.

        """
        # Set the retry config if it differs from current config.
        self._session = http_session.set_retry_config(
            self._session,
            max_retries=max_retries,
            status_forcelist=retry_status,
            backoff_factor=backoff_factor,
            )

        response = self._predict(x, compress, prediction_id)
        id = response.headers.get('verta-request-id', '')
        return (id, _utils.body_to_json(response))


    # TODO: Remove this method after release of 0.22.0
    @classmethod
    def from_url(cls, url, token=None, creds=None):
        """
        Returns a :class:`DeployedModel` based on a custom URL and token.

        .. deprecated:: 0.22.0
           For most use cases, call :meth:`Endpoint.get_deployed_model() <verta.endpoint.Endpoint.get_deployed_model>`.
           For lower-level needs, call the ``DeployedModel`` class directly with the same parameters.
        """
        warnings.warn(
            "This method is deprecated and will be removed in an upcoming version;"
            " Drop \".from_url\" and call the DeployedModel class directly with the same parameters.",
            category=FutureWarning,
            )
        return cls(
            prediction_url=url,
            token=token,
            creds=creds,
            )


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
