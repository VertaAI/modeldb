# -*- coding: utf-8 -*-

import datetime
import glob
import inspect
import itertools
import json
import logging
import numbers
import os
import re
import site
import string
import sys
import threading
import time
from typing import Optional, Union
from urllib.parse import urljoin
import warnings

import click
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from google.protobuf import json_format
from google.protobuf.struct_pb2 import Value, ListValue, Struct, NULL_VALUE

from .._vendored import six

from verta.credentials import EmailCredentials

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.uac import UACService_pb2, Workspace_pb2

from . import importer

logger = logging.getLogger(__name__)


_GRPC_PREFIX = "Grpc-Metadata-"

_VALID_HTTP_METHODS = {"GET", "POST", "PUT", "DELETE", "PATCH"}
_VALID_FLAT_KEY_CHARS = set(string.ascii_letters + string.digits + "_-/")

THREAD_LOCALS = threading.local()
THREAD_LOCALS.active_experiment_run = None


class Connection(object):
    _OSS_DEFAULT_WORKSPACE = "personal"

    def __init__(
        self,
        scheme=None,
        socket=None,
        auth=None,
        max_retries=0,
        ignore_conn_err=False,
        credentials=None,
        headers=None,
    ):
        """
        HTTP connection configuration utility struct.

        Parameters
        ----------
        scheme : {'http', 'https'}, optional
            HTTP authentication scheme.
        socket : str, optional
            Hostname and port.
        auth : dict, optional
            Verta authentication headers.
        max_retries : int, default 0
            Maximum number of times to retry a request on a connection failure. This only attempts retries
            on HTTP codes {502, 503, 504} which commonly occur during back end connection lapses.
        ignore_conn_err : bool, default False
            Whether to ignore connection errors and instead return successes with empty contents.
        credentials : :class:`~verta.credentials.Credentials`, optional
            Either dev key or JWT token data to be used for authentication.
        headers: dict, optional
            Additional headers to attach to requests.

        """
        self._init_headers()
        self.scheme = scheme
        self.socket = socket
        # TODO: retry on 404s, but only if we're sure it's not legitimate e.g. from a GET
        self.retry = Retry(
            total=max_retries,
            backoff_factor=1,  # each retry waits (2**retry_num) seconds
            allowed_methods=False,  # retry on all HTTP methods
            status_forcelist=(
                requests.codes.bad_gateway,
                requests.codes.unavailable,
                requests.codes.gateway_timeout,
            ),  # only retry on these status codes
            raise_on_redirect=False,  # return Response instead of raising after max retries
            raise_on_status=False,
        )  # return Response instead of raising after max retries
        self.ignore_conn_err = ignore_conn_err
        self.credentials = credentials
        self.headers = headers

    @property
    def credentials(self):
        return self._credentials

    @credentials.setter
    def credentials(self, value):
        self._credentials = value
        self._recompute_headers()

    @property
    def headers(self):
        return self._computed_headers

    # Note: Added for temporary backwards compatibility. Remove when possible.
    @property
    def auth(self):
        return self.headers

    @headers.setter
    def headers(self, value):
        self._headers = value or dict()
        self._recompute_headers()

    def _init_headers(self):
        self._headers = {}
        self._computed_headers = {}

    def _recompute_headers(self):
        headers = self._headers or dict()
        headers = headers.copy()
        headers[_GRPC_PREFIX + "scheme"] = self.scheme
        headers.update(self.prefixed_headers_for_credentials(self.credentials))
        self._computed_headers = headers

    @staticmethod
    def prefixed_headers_for_credentials(credentials):
        if credentials:
            return {(_GRPC_PREFIX + k): v for (k, v) in credentials.headers().items()}
        return {}

    def test(self, print_success=True):
        """Verify connection viability with Verta Platform.

        This method issues an API request against the Verta platform using the
        configuration in this connection to validate that the Verta platform can
        be connected to.

        Parameters
        ----------
        print_success : bool, default True
            Whether or not to print a success message.

        Returns
        -------
        bool
            Returns true upon success.

        Raises
        ------
        :class:`requests.HTTPError`
            If an HTTP error occured.

        """
        try:
            response = make_request(
                "GET",
                "{}://{}/api/v1/modeldb/project/verifyConnection".format(
                    self.scheme, self.socket
                ),
                self,
            )
        except requests.ConnectionError as err:
            err.args = (
                "connection failed; please check `host` and `port`; error message: \n\n{}".format(
                    err.args[0]
                ),
            ) + err.args[1:]
            six.raise_from(err, None)

        if response.status_code in (
            requests.codes.unauthorized,
            requests.codes.forbidden,
        ):
            try:
                response.raise_for_status()
            except requests.HTTPError as e:
                e.args = (
                    "authentication failed; please check `VERTA_EMAIL` and `VERTA_DEV_KEY` or JWT credentials\n\n{}".format(
                        e.args[0]
                    ),
                ) + e.args[1:]
                raise e
        raise_for_http_error(response)
        if print_success:
            print("connection successfully established")
        return True

    def make_proto_request(
        self, method, path, params=None, body=None, include_default=True
    ):
        if params is not None:
            params = proto_to_json(params)
        if body is not None:
            body = proto_to_json(body, include_default)
        response = make_request(
            method,
            "{}://{}{}".format(self.scheme, self.socket, path),
            self,
            params=params,
            json=body,
        )

        return response

    @staticmethod
    def maybe_proto_response(response, response_type):
        if response.ok:
            response_msg = json_to_proto(body_to_json(response), response_type)
            return response_msg
        else:
            if (
                response.status_code == requests.codes.forbidden
                and body_to_json(response)["code"] == 7
            ) or (
                response.status_code == requests.codes.not_found
                and body_to_json(response)["code"] == 5
            ):
                return NoneProtoResponse()
            else:
                raise_for_http_error(response)

    @staticmethod
    def must_proto_response(response, response_type):
        if response.ok:
            response_msg = json_to_proto(body_to_json(response), response_type)
            return response_msg
        else:
            raise_for_http_error(response)

    @staticmethod
    def must_response(response):
        raise_for_http_error(response)

    @staticmethod
    def is_html_response(response):
        content_type = response.headers.get("Content-Type")
        if content_type:
            return content_type.startswith("text/html")
        return False

    @property
    def email(self):
        if self.credentials and isinstance(self.credentials, EmailCredentials):
            return self.credentials.email
        else:
            return None

    def _get_visible_workspaces(self):
        response = self.make_proto_request(
            "GET", "/api/v1/uac-proxy/workspace/getVisibleWorkspaces"
        )
        response = self.must_proto_response(response, Workspace_pb2.Workspaces)

        org_names = map(lambda workspace: workspace.org_name, response.workspace)
        org_names = filter(None, org_names)
        return list(org_names)

    def _get_organization_id(self):
        response = self.make_proto_request(
            "GET", "/api/v1/uac-proxy/workspace/getVisibleWorkspaces"
        )
        response = self.must_proto_response(response, Workspace_pb2.Workspaces)

        workspace_names = map(lambda workspace: workspace.org_id, response.workspace)
        workspace_names = filter(None, workspace_names)
        return list(workspace_names)[0]

    def _set_default_workspace(self, name):
        msg = Workspace_pb2.GetWorkspaceByName(name=name)
        response = self.make_proto_request(
            "GET", "/api/v1/uac-proxy/workspace/getWorkspaceByName", params=msg
        )
        workspace = self.must_proto_response(response, Workspace_pb2.Workspace)

        response = self.make_proto_request(
            "GET", "/api/v1/uac-proxy/uac/getCurrentUser"
        )
        user_info = self.must_proto_response(response, UACService_pb2.UserInfo)

        msg = UACService_pb2.UpdateUser(
            info=user_info, default_workspace_id=workspace.id
        )
        response = self.make_proto_request(
            "POST", "/api/v1/uac-proxy/uac/updateUser", body=msg
        )
        raise_for_http_error(response)

    def is_workspace(self, workspace_name):
        msg = Workspace_pb2.GetWorkspaceByName(name=workspace_name)
        response = self.make_proto_request(
            "GET", "/api/v1/uac-proxy/workspace/getWorkspaceByName", params=msg
        )

        return response.ok

    def get_workspace_name_from_id(self, workspace_id):
        """For registry, which uses workspace service."""
        msg = Workspace_pb2.GetWorkspaceById(id=int(workspace_id))
        response = self.make_proto_request(
            "GET", "/api/v1/uac-proxy/workspace/getWorkspaceById", params=msg
        )

        workspace = self.must_proto_response(response, Workspace_pb2.Workspace)
        return workspace.username or workspace.org_name

    def get_default_workspace(self):
        response = self.make_proto_request(
            "GET", "/api/v1/uac-proxy/uac/getCurrentUser"
        )

        if (
            response.ok and self.is_html_response(response)
        ) or response.status_code == 404:  # fetched webapp  # UAC not found
            return self._OSS_DEFAULT_WORKSPACE

        user_info = self.must_proto_response(response, UACService_pb2.UserInfo)
        workspace_id = user_info.verta_info.default_workspace_id
        if workspace_id:
            return self.get_workspace_name_from_id(workspace_id)
        else:
            raise RuntimeError("default workspace is not set")


class NoneProtoResponse(object):
    def __init__(self):
        pass

    def __getattr__(self, item):
        return None

    def HasField(self, name):
        return False


class Configuration(object):
    def __init__(self, use_git=True, debug=False):
        """
        Client behavior configuration utility struct.

        Parameters
        ----------
        use_git : bool, default True
            Whether to use a local Git repository for certain operations.

        """
        self.use_git = use_git
        self.debug = debug


def make_request(method, url, conn, stream=False, **kwargs):
    """
    Makes a REST request.

    Parameters
    ----------
    method : {'GET', 'POST', 'PUT', 'DELETE'}
        HTTP method.
    url : str
        URL.
    conn : :class:`Connection`
        Connection authentication and configuration.
    stream : bool, default False
        Whether to stream the response contents.
    **kwargs
        Initialization arguments to :class:`requests.Request`.

    Returns
    -------
    :class:`requests.Response`

    """
    if method.upper() not in _VALID_HTTP_METHODS:
        raise ValueError("`method` must be one of {}".format(_VALID_HTTP_METHODS))

    # add auth to headers
    kwargs.setdefault("headers", {}).update(conn.headers)

    with requests.Session() as session:
        session.mount(url, HTTPAdapter(max_retries=conn.retry))
        try:
            request = requests.Request(method, url, **kwargs).prepare()

            # retry loop for broken connections
            MAX_RETRIES = conn.retry.total
            for retry_num in range(MAX_RETRIES + 1):
                logger.debug("Making request ({} retries)".format(retry_num))
                try:
                    response = _make_request(
                        session, request, conn.ignore_conn_err, stream=stream
                    )
                except requests.ConnectionError as e:
                    if (retry_num == MAX_RETRIES) or ("BrokenPipeError" not in str(e)):
                        if not conn.ignore_conn_err:
                            raise e
                        else:
                            return fabricate_200()
                    time.sleep(1)
                else:
                    break

        except (
            requests.exceptions.BaseHTTPError,
            requests.exceptions.RequestException,
        ) as e:
            if not conn.ignore_conn_err:
                raise e
            # else fall through to fabricate 200 response
        else:
            if response.ok or not conn.ignore_conn_err:
                return response
            # else fall through to fabricate 200 response
        return fabricate_200()


def _make_request(session, request, ignore_conn_err=False, **kwargs):
    """
    Actually sends the request across the wire, and resolves non-302 redirects.

    Parameters
    ----------
    session : :class:`requests.Session`
        Connection-pooled session for making requests.
    request : :class:`requests.PreparedRequest`
        Request to make.
    ignore_conn_err : bool, default False
        Whether to ignore connection errors and instead return a success with empty contents.
    **kwargs
        Arguments to :meth:`requests.Session.send`

    Returns
    -------
    :class:`requests.Response`

    """
    # Our use of Session.send() with a PreparedRequest inadvertently bypasses
    # requests's mechanisms for merging env vars, Session attrs, and params.
    # They recommend calling Session.merge_environment_settings() manually
    # to pick up values such as the `REQUESTS_CA_BUNDLE` env var.
    # https://2.python-requests.org/en/master/user/advanced/#prepared-requests
    # TODO: switch over to Session.request() (VR-12458)
    kwargs.update(
        session.merge_environment_settings(
            url=request.url,
            proxies=kwargs.get("proxies", {}),
            stream=kwargs.get("stream"),
            verify=kwargs.get("verify"),
            cert=kwargs.get("cert"),
        )
    )

    response = session.send(request, allow_redirects=False, **kwargs)

    # manually inspect initial response and subsequent redirects to stop on 302s
    history = (
        []
    )  # track history because `requests` doesn't since we're redirecting manually
    responses = itertools.chain(
        [response], session.resolve_redirects(response, request)
    )
    for response in responses:
        if response.status_code == 302:
            if not ignore_conn_err:
                raise RuntimeError(
                    "received status 302 from {},"
                    " which is not supported by the Client".format(response.url)
                )
            else:
                return fabricate_200()

        history.append(response)
    # set full history
    response.history = history[:-1]  # last element is this response, so drop it

    return response


def fabricate_200():
    """
    Returns an HTTP response with ``status_code`` 200 and empty JSON contents.

    This is used when the Client has ``ignore_conn_err=True``, so that backend responses can be
    spoofed to minimize execution-halting errors.

    Returns
    -------
    :class:`requests.Response`

    """
    response = requests.Response()
    response.status_code = 200  # success
    response._content = six.ensure_binary("{}")  # empty contents
    return response


def raise_for_http_error(response: requests.Response):
    """
    Raises a potential HTTP error with a back end message if provided, or a default error message otherwise.

    Parameters
    ----------
    response : :class:`requests.Response`
        Response object returned from a `requests`-module HTTP request.

    Raises
    ------
    :class:`requests.HTTPError`
        If an HTTP error occured.

    """
    try:
        response.raise_for_status()
    except requests.HTTPError as e:
        # get current time in UTC to display alongside exception
        time_str = f" at {timestamp_to_str(now(), utc=True)} UTC"

        try:
            reason = body_to_json(response)
        except ValueError:
            reason = response.text.strip()  # response is not json

        if isinstance(reason, dict):
            if "message" in reason:
                reason = reason["message"]
            else:
                # fall back to entire text
                reason = response.text.strip()

        if not reason:
            e.args = (e.args[0] + time_str,) + e.args[
                1:
            ]  # attach time to error message
            six.raise_from(e, None)  # use default reason
        else:
            # replicate https://github.com/psf/requests/blob/428f7a/requests/models.py#L954
            if 400 <= response.status_code < 500:
                cause = "Client"
            elif 500 <= response.status_code < 600:
                cause = "Server"
            else:  # should be impossible here, but sure okay
                cause = "Unexpected"
            message = (
                f"{response.status_code} {cause} Error: {reason} "
                f"for url: {response.url}{time_str}"
            )
            six.raise_from(requests.HTTPError(message, response=response), None)


def body_to_json(response):
    """
    Returns the JSON-encoded contents of `response`, raising a detailed error on failure.

    Parameters
    ----------
    response : :class:`requests.Response`
        HTTP response.

    Returns
    -------
    contents : dict
        JSON-encoded contents of `response`.

    Raises
    ------
    ValueError
        If `response`'s contents are not JSON-encoded.

    """
    try:
        return response.json()
    except ValueError:  # not JSON response
        msg = "\n".join(
            [
                "expected JSON response from {}, but instead got:".format(response.url),
                response.text or "<empty response>",
                "",
                "Please notify the Verta development team.",
            ]
        )
        msg = six.ensure_str(msg)
        six.raise_from(ValueError(msg), None)


def is_in_venv(path):
    # Roughly checks for:
    #     /
    #     |_ lib/
    #     |   |_ python*/ <- directory with Python packages, containing `path`
    #     |
    #     |_ bin/
    #         |_ python*  <- Python executable
    for py_lib_dir in [
        os.path.join(os.sep, "lib", "python"),
        os.path.join(os.sep, "lib32", "python"),
        os.path.join(os.sep, "lib64", "python"),  # https://stackoverflow.com/q/11370877
        os.path.join(os.sep, "bin", "__pycache__"),
    ]:
        i = path.find(py_lib_dir)
        if i != -1 and glob.glob(os.path.join(path[:i], "bin", "python*")):
            return True

    # Debian's system-level packages from apt
    #     https://wiki.debian.org/Python#Deviations_from_upstream
    dist_pkg_pattern = re.compile(r"/usr(/local)?/lib/python[0-9.]+/dist-packages")
    if dist_pkg_pattern.match(path):
        return True

    # packages installed via --user
    if path.startswith(site.USER_SITE):
        return True

    return False


def is_hidden(path):  # to avoid "./".startswith('.')
    return os.path.basename(path.rstrip("/")).startswith(".") and path != "."


def find_filepaths(
    paths, extensions=None, include_hidden=False, include_venv=False, followlinks=True
):
    """
    Unravels a list of file and directory paths into a list of only filepaths by walking through the
    directories.

    Parameters
    ----------
    paths : str or list of str
        File and directory paths.
    extensions : str or list of str, optional
        What files to include while walking through directories. If not provided, all files will be
        included.
    include_hidden : bool, default False
        Whether to include hidden files and subdirectories found while walking through directories.
    include_venv : bool, default False
        Whether to include Python virtual environment directories.

    Returns
    -------
    filepaths : set

    """
    if isinstance(paths, six.string_types):
        paths = [paths]
    paths = list(map(os.path.expanduser, paths))

    if isinstance(extensions, six.string_types):
        extensions = [extensions]
    if extensions is not None:
        # prepend period to file extensions where missing
        extensions = map(
            lambda ext: ext if ext.startswith(".") else ("." + ext), extensions
        )
        extensions = set(extensions)

    filepaths = set()
    for path in paths:
        if os.path.isdir(path):
            for parent_dir, dirnames, filenames in os.walk(
                path, followlinks=followlinks
            ):
                if not include_hidden:
                    # skip hidden directories
                    dirnames[:] = [
                        dirname for dirname in dirnames if not is_hidden(dirname)
                    ]
                    # skip hidden files
                    filenames[:] = [
                        filename for filename in filenames if not is_hidden(filename)
                    ]

                # If we don't want to include venvs, there are the following scenarios for us:
                # 1) the path passed is a venv but we explicitly asked for that path, so it should be included
                # 2) the path passed isn't in a venv, but it has a venv inside of it, in which case we should skip the venv part
                # 3) there is no venv anywhere in the path, in which case nothing changes
                if not include_venv and not is_in_venv(path) and is_in_venv(parent_dir):
                    continue

                for filename in filenames:
                    if (
                        extensions is None
                        or os.path.splitext(filename)[1] in extensions
                    ):
                        filepaths.add(os.path.join(parent_dir, filename))
        else:
            filepaths.add(path)

    return filepaths


def proto_to_json(msg, include_default=True):
    """
    Converts a `protobuf` `Message` object into a JSON-compliant dictionary.

    The output preserves snake_case field names and integer representaions of enum variants.

    Parameters
    ----------
    msg : google.protobuf.message.Message
        `protobuf` `Message` object.

    Returns
    -------
    dict
        JSON object representing `msg`.

    """
    return json.loads(
        json_format.MessageToJson(
            msg,
            including_default_value_fields=include_default,
            preserving_proto_field_name=True,
            use_integers_for_enums=True,
        )
    )


def json_to_proto(response_json, response_cls, ignore_unknown_fields=True):
    """
    Converts a JSON-compliant dictionary into a `protobuf` `Message` object.

    Parameters
    ----------
    response_json : dict
        JSON object representing a Protocol Buffer message.
    response_cls : type
        `protobuf` `Message` subclass, e.g. ``CreateProject.Response``.
    ignore_unknown_fields : bool, default True
        Whether to allow (and ignore) fields in `response_json` that are not defined in
        `response_cls`. This is for forward compatibility with the back end; if the Client protos
        are outdated and we get a response with new fields, ``True`` prevents an error.

    Returns
    -------
    google.protobuf.message.Message
        `protobuf` `Message` object represented by `response_json`.

    """
    return json_format.Parse(
        json.dumps(response_json),
        response_cls(),
        ignore_unknown_fields=ignore_unknown_fields,
    )


def get_bool_types():
    """
    Determines available bool types (including NumPy's ``bool_`` if importable) for typechecks.

    Returns
    -------
    tuple
        Available bool types.

    """
    np = importer.maybe_dependency("numpy")
    if np is None:
        return (bool,)
    else:
        return (bool, np.bool_)


def to_builtin(obj):
    """
    Tries to coerce `obj` into a built-in type, for JSON serialization.

    Parameters
    ----------
    obj

    Returns
    -------
    object
        A built-in equivalent of `obj`, or `obj` unchanged if it could not be handled by this function.

    """
    # jump through ludicrous hoops to avoid having hard dependencies in the Client
    cls_ = obj.__class__
    obj_class = getattr(cls_, "__name__", None)
    obj_module = getattr(cls_, "__module__", None)

    # booleans
    if isinstance(obj, get_bool_types()):
        return True if obj else False

    # NumPy scalars
    if obj_module == "numpy" and obj_class.startswith(("int", "uint", "float", "str")):
        return obj.item()

    # scientific library collections
    if obj_class == "ndarray":
        return obj.tolist()
    if obj_class == "Series":
        return obj.values.tolist()
    if obj_class == "DataFrame":
        return obj.values.tolist()
    if obj_class == "Tensor" and obj_module == "torch":
        return obj.detach().numpy().tolist()
    tf = importer.maybe_dependency("tensorflow")
    if tf is not None and isinstance(obj, tf.Tensor):  # if TensorFlow
        try:
            return obj.numpy().tolist()
        except:  # TF 1.X or not-eager execution
            pass

    # strings
    if isinstance(obj, six.string_types):  # prevent infinite loop with iter
        return obj
    if isinstance(obj, six.binary_type):
        return six.ensure_str(obj)

    # dicts and lists
    if isinstance(obj, dict):
        return {to_builtin(key): to_builtin(val) for key, val in six.viewitems(obj)}
    try:
        iter(obj)
    except TypeError:
        pass
    else:
        return [to_builtin(val) for val in obj]

    return obj


def python_to_val_proto(raw_val, allow_collection=False):
    """
    Converts a Python variable into a `protobuf` `Value` `Message` object.

    Parameters
    ----------
    raw_val
        Python variable.
    allow_collection : bool, default False
        Whether to allow ``list``s and ``dict``s as `val`. This flag exists because some callers
        ought to not support logging collections, so this function will perform the typecheck on `val`.

    Returns
    -------
    google.protobuf.struct_pb2.Value
        `protobuf` `Value` `Message` representing `val`.

    """
    # TODO: check `allow_collection` before `to_builtin()` to avoid unnecessary processing
    val = to_builtin(raw_val)

    if val is None:
        return Value(null_value=NULL_VALUE)
    elif isinstance(val, bool):  # did you know that `bool` is a subclass of `int`?
        return Value(bool_value=val)
    elif isinstance(val, numbers.Real):
        return Value(number_value=val)
    elif isinstance(val, six.string_types):
        return Value(string_value=val)
    elif isinstance(val, (list, dict)):
        if allow_collection:
            if isinstance(val, list):
                list_value = ListValue()
                list_value.extend(val)  # pylint: disable=no-member
                return Value(list_value=list_value)
            else:  # isinstance(val, dict)
                if all([isinstance(key, six.string_types) for key in val.keys()]):
                    struct_value = Struct()
                    struct_value.update(val)  # pylint: disable=no-member
                    return Value(struct_value=struct_value)
                else:  # protobuf's fault
                    raise TypeError(
                        "struct keys must be strings; consider using log_artifact() instead"
                    )
        else:
            raise TypeError(
                "unsupported type {}; consider using log_attribute() instead".format(
                    type(raw_val)
                )
            )
    else:
        raise TypeError(
            "unsupported type {}; consider using log_artifact() instead".format(
                type(raw_val)
            )
        )


def val_proto_to_python(msg):
    """
    Converts a `protobuf` `Value` `Message` object into a Python variable.

    Parameters
    ----------
    msg : google.protobuf.struct_pb2.Value
        `protobuf` `Value` `Message` representing a variable.

    Returns
    -------
    one of {None, bool, float, int, str}
        Python variable represented by `msg`.

    """
    value_kind = msg.WhichOneof("kind")
    if value_kind == "null_value":
        return None
    elif value_kind == "bool_value":
        return msg.bool_value
    elif value_kind == "number_value":
        return (
            int(msg.number_value) if msg.number_value.is_integer() else msg.number_value
        )
    elif value_kind == "string_value":
        return msg.string_value
    elif value_kind == "list_value":
        return [val_proto_to_python(val_msg) for val_msg in msg.list_value.values]
    elif value_kind == "struct_value":
        return {
            key: val_proto_to_python(val_msg)
            for key, val_msg in msg.struct_value.fields.items()
        }
    else:
        raise NotImplementedError("retrieved value type is not supported")


def unravel_key_values(rpt_key_value_msg):
    """
    Converts a repeated KeyValue field of a protobuf message into a dictionary.

    Parameters
    ----------
    rpt_key_value_msg : google.protobuf.pyext._message.RepeatedCompositeContainer
        Repeated KeyValue field of a protobuf message.

    Returns
    -------
    dict of str to {None, bool, float, int, str}
        Names and values.

    """
    return {
        key_value.key: val_proto_to_python(key_value.value)
        for key_value in rpt_key_value_msg
    }


def unravel_artifacts(rpt_artifact_msg):
    """
    Converts a repeated Artifact field of a protobuf message into a list of names.

    Parameters
    ----------
    rpt_artifact_msg : google.protobuf.pyext._message.RepeatedCompositeContainer
        Repeated Artifact field of a protobuf message.

    Returns
    -------
    list of str
        Names of artifacts.

    """
    return [artifact.key for artifact in rpt_artifact_msg]


def unravel_observation(obs_msg):
    """
    Converts an Observation protobuf message into a more straightforward Python tuple.

    This is useful because an Observation message has a oneof that's finicky to handle.

    Returns
    -------
    str
        Name of observation.
    {None, bool, float, int, str}
        Value of observation.
    str
        Human-readable timestamp.

    """
    if obs_msg.WhichOneof("oneOf") == "attribute":
        key = obs_msg.attribute.key
        value = obs_msg.attribute.value
    elif obs_msg.WhichOneof("oneOf") == "artifact":
        key = obs_msg.artifact.key
        value = "{} artifact".format(
            _CommonCommonService.ArtifactTypeEnum.ArtifactType.Name(
                obs_msg.artifact.artifact_type
            )
        )
    return (
        key,
        val_proto_to_python(value),
        timestamp_to_str(obs_msg.timestamp),
        int(obs_msg.epoch_number.number_value),
    )


def unravel_observations(rpt_obs_msg):
    """
    Converts a repeated Observation field of a protobuf message into a dictionary.

    Parameters
    ----------
    rpt_obs_msg : google.protobuf.pyext._message.RepeatedCompositeContainer
        Repeated Observation field of a protobuf message.

    Returns
    -------
    dict of str to list of tuples ({None, bool, float, int, str}, str)
        Names and observation sequences.

    """
    observations = {}
    for obs_msg in rpt_obs_msg:
        obs_tuple = unravel_observation(obs_msg)
        key = obs_tuple[0]
        observations.setdefault(key, []).append(obs_tuple[1:])
    return observations


def validate_flat_key(key):
    """
    Checks whether `key` contains invalid characters.

    To prevent bugs with querying (which allow dot-delimited nested keys), flat keys (such as those
    used for individual metrics) must not contain periods.

    Furthermore, to prevent potential bugs with the back end down the line, keys should be restricted
    to alphanumeric characters, underscores, and dashes until we can verify robustness.

    Parameters
    ----------
    key : str
        Name of metadatum.

    Raises
    ------
    ValueError
        If `key` contains invalid characters.

    """
    for c in key:
        if c not in _VALID_FLAT_KEY_CHARS:
            raise ValueError(
                "`key` may only contain alphanumeric characters, underscores, dashes,"
                " and forward slashes"
            )


def generate_default_name():
    """
    Generates a string that can be used as a default entity name while avoiding collisions.

    The generated string is a concatenation of the current process ID and the current Unix timestamp,
    such that a collision should only occur if a single process produces two of an entity at the same
    nanosecond.

    Returns
    -------
    name : str
        String generated from the current process ID and Unix timestamp.

    """
    return "{}{}".format(os.getpid(), str(time.time()).replace(".", ""))


class UTC(datetime.tzinfo):
    """UTC timezone class for Python 2 timestamp calculations"""

    def utcoffset(self, dt):
        return datetime.timedelta(0)

    def tzname(self, dt):
        return "UTC"

    def dst(self, dt):
        return datetime.timedelta(0)


def timestamp_to_ms(timestamp):
    """
    Converts a Unix timestamp into one with millisecond resolution.

    Parameters
    ----------
    timestamp : float or int
        Unix timestamp.

    Returns
    -------
    int
        `timestamp` with millisecond resolution (13 integer digits).

    """
    num_integer_digits = len(str(timestamp).split(".")[0])
    return int(timestamp * 10 ** (13 - num_integer_digits))


def ensure_timestamp(timestamp):
    """
    Converts a representation of a datetime into a Unix timestamp with millisecond resolution.

    If `timestamp` is provided as a string, this function attempts to use pandas (if installed) to
    parse it into a Unix timestamp, since pandas can interally handle many different human-readable
    datetime string representations. If pandas is not installed, this function will only handle an
    ISO 8601 representation.

    Parameters
    ----------
    timestamp : str or float or int
        String representation of a datetime or numerical Unix timestamp.

    Returns
    -------
    int
        `timestamp` with millisecond resolution (13 integer digits).

    """
    if isinstance(timestamp, six.string_types):
        pd = importer.maybe_dependency("pandas")
        if pd is not None:
            try:  # attempt with pandas, which can parse many time string formats
                return timestamp_to_ms(pd.Timestamp(timestamp).timestamp())
            except ValueError:  # can't be handled by pandas
                six.raise_from(
                    ValueError(
                        'unable to parse datetime string "{}"'.format(timestamp)
                    ),
                    None,
                )
        else:
            six.raise_from(
                ValueError("pandas must be installed to parse datetime strings"), None
            )
    elif isinstance(timestamp, numbers.Real):
        return timestamp_to_ms(timestamp)
    elif isinstance(timestamp, datetime.datetime):
        seconds = timestamp.timestamp()
        return timestamp_to_ms(seconds)
    else:
        raise TypeError("unable to parse timestamp of type {}".format(type(timestamp)))


def timestamp_to_str(timestamp, utc=False):
    """
    Converts a Unix timestamp into a human-readable string representation.

    Parameters
    ----------
    timestamp : int
        Numerical Unix timestamp.

    Returns
    -------
    str
        Human-readable string representation of `timestamp`.

    """
    num_digits = len(str(timestamp))
    ts_as_sec = timestamp * 10 ** (10 - num_digits)
    if utc:
        datetime_obj = datetime.datetime.utcfromtimestamp(ts_as_sec)
    else:
        datetime_obj = datetime.datetime.fromtimestamp(ts_as_sec)
    return str(datetime_obj)


def now():
    """
    Returns the current Unix timestamp with millisecond resolution.

    Returns
    -------
    now : int
        Current Unix timestamp in milliseconds.

    """
    return timestamp_to_ms(time.time())


def get_python_version():
    """
    Returns the version number of the locally-installed Python interpreter.

    Returns
    -------
    str
        Python version number in the form "{major}.{minor}.{patch}".

    """
    return ".".join(map(str, sys.version_info[:3]))


def save_notebook(notebook_path=None, timeout=5):
    """
    Saves the current notebook on disk and returns its contents after the file has been rewritten.

    Parameters
    ----------
    notebook_path : str, optional
        Filepath of the Jupyter Notebook.
    timeout : float, default 5
        Maximum number of seconds to wait for the notebook to save.

    Returns
    -------
    notebook_contents : file-like
        An in-memory copy of the notebook's contents at the time this function returns. This can
        be ignored, but is nonetheless available to minimize the risk of a race condition caused by
        delaying the read until a later time.

    Raises
    ------
    OSError
        If the notebook is not saved within `timeout` seconds.

    """
    IPython_display = importer.maybe_dependency("IPython.display")
    if IPython_display is None:
        raise ImportError("unable to import libraries necessary for saving notebook")

    if notebook_path is None:
        notebook_path = get_notebook_filepath()
    modtime = os.path.getmtime(notebook_path)

    IPython_display.display(
        IPython_display.Javascript(
            """
    require(["base/js/namespace"],function(Jupyter) {
        Jupyter.notebook.save_checkpoint();
    });
    """
        )
    )

    # wait for file to be modified
    start_time = time.time()
    while time.time() - start_time < timeout:
        new_modtime = os.path.getmtime(notebook_path)
        if new_modtime > modtime:
            break
        time.sleep(0.01)
    else:
        raise OSError("unable to save notebook")

    # wait for file to be rewritten
    timeout -= time.time() - start_time  # remaining time
    start_time = time.time()
    while time.time() - start_time < timeout:
        with open(notebook_path, "r") as f:
            contents = f.read()
        if contents:
            return six.StringIO(contents)
        time.sleep(0.01)
    else:
        raise OSError("unable to read saved notebook")


def get_notebook_filepath():
    """
    Returns the filesystem path of the Jupyter notebook running the Client.

    This implementation is from https://github.com/jupyter/notebook/issues/1000#issuecomment-359875246.

    Returns
    -------
    str

    Raises
    ------
    OSError
        If one of the following is true:
            - Jupyter is not installed
            - Client is not being called from a notebook
            - the calling notebook cannot be identified

    """
    ipykernel = importer.maybe_dependency("ipykernel")
    if ipykernel is None:
        raise ImportError("unable to import libraries necessary for locating notebook")

    notebookapp = importer.maybe_dependency("notebook.notebookapp")
    if notebookapp is None:
        # Python 2, util we need is in different module
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            notebookapp = importer.maybe_dependency("IPython.html.notebookapp")
    if notebookapp is None:  # abnormally nonstandard installation of Jupyter
        raise ImportError("unable to import libraries necessary for locating notebook")

    try:
        connection_file = ipykernel.connect.get_connection_file()
    except (NameError, RuntimeError):  # Jupyter not installed  # not in a Notebook
        pass
    else:
        kernel_id = re.search("kernel-(.*).json", connection_file).group(1)
        for server in notebookapp.list_running_servers():
            response = requests.get(
                urljoin(server["url"], "api/sessions"),
                params={"token": server.get("token", "")},
            )
            if response.ok:
                for session in body_to_json(response):
                    if session["kernel"]["id"] == kernel_id:
                        relative_path = session["notebook"]["path"]
                        return os.path.join(server["notebook_dir"], relative_path)
    raise OSError("unable to find notebook file")


def get_script_filepath():
    """
    Returns the filesystem path of the Python script running the Client.

    This function iterates back through the call stack until it finds a non-Verta stack frame and
    returns its filepath.

    Returns
    -------
    str

    Raises
    ------
    OSError
        If the calling script cannot be identified.

    """
    for frame_info in inspect.stack():
        module = inspect.getmodule(frame_info[0])
        if module is None or module.__name__.split(".", 1)[0] != "verta":
            filepath = frame_info[1]
            if os.path.exists(filepath):  # e.g. Jupyter fakes the filename for cells
                return filepath
            else:
                break  # continuing might end up returning a built-in
    raise OSError("unable to find script file")


def is_org(workspace_name, conn):
    response = make_request(
        "GET",
        "{}://{}/api/v1/uac-proxy/organization/getOrganizationByName".format(
            conn.scheme, conn.socket
        ),
        conn,
        params={"org_name": workspace_name},
    )

    return response.status_code != 404


def as_list_of_str(tags):
    """
    Ensures that `tags` is a list of str.

    Parameters
    ----------
    tags : str or list of str
        If list of str, return unchanged. If str, return wrapped in a list.

    Returns
    -------
    tags : list of str
        Tags.

    Raises
    ------
    TypeError
        If `tags` is neither str nor list of str.

    """
    # TODO: make error messages more general so this can be used for any similar var
    if isinstance(tags, six.string_types):
        tags = [tags]
    else:
        if not isinstance(tags, (list, tuple, set)):
            raise TypeError("`tags` should be list of str, not {}".format(type(tags)))

        for tag in tags:
            if not isinstance(tag, six.string_types):
                raise TypeError(
                    "`tags` must be list of str, but found {}".format(type(tag))
                )

    return tags


def _multiple_arguments_for_each(argument, name, action, get_keys, overwrite):
    name = name
    argument = list(map(lambda s: s.split("="), argument))
    if argument and len(argument) > len(set(map(lambda pair: pair[0], argument))):
        raise click.BadParameter("cannot have duplicate {} keys".format(name))
    if argument:
        argument_keys = set(get_keys())

        for pair in argument:
            if len(pair) != 2:
                raise click.BadParameter(
                    "key and path for {}s must be separated by a '='".format(name)
                )
            (key, _) = pair
            if key == "model":
                raise click.BadParameter('the key "model" is reserved for model')

            if not overwrite and key in argument_keys:
                raise click.BadParameter(
                    'key "{}" already exists; consider using --overwrite flag'.format(
                        key
                    )
                )

        for key, path in argument:
            action(key, path)


def check_unnecessary_params_warning(resource_name, name, param_names, params):
    if any(param is not None for param in params):
        warnings.warn(
            "{} with {} already exists;"
            " cannot set {}".format(resource_name, name, param_names)
        )
