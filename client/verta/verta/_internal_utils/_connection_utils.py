# -*- coding: utf-8 -*-

import os

from ..external.six.moves.urllib.parse import urlparse  # pylint: disable=import-error, no-name-in-module

from . import _utils


# TODO: move connection and request utils from _utils to here


def establish_connection(host=None, email=None, dev_key=None,
                         max_retries=0, ignore_conn_err=False):
    config = {}  # TODO: load config

    # pick up `host`
    if host is None and 'VERTA_HOST' in os.environ:
        host = os.environ['VERTA_HOST']
        print("set host from environment")
    if host is None and 'host' in config:
        host = config['host']
        print("set host from config")
    if host is None:
        raise ValueError("`host` must be provided")

    # pick up `email`
    if email is None and 'VERTA_EMAIL' in os.environ:
        email = os.environ['VERTA_EMAIL']
        print("set email from environment")
    if email is None and 'email' in config:
        host = config['email']
        print("set host from config")

    # pick up `dev_key`
    if dev_key is None and 'VERTA_DEV_KEY' in os.environ:
        dev_key = os.environ['VERTA_DEV_KEY']
        print("set developer key from environment")
    if dev_key is None and 'dev_key' in config:
        host = config['dev_key']
        print("set host from config")

    # parse `host` into scheme and socket
    back_end_url = urlparse(host)
    scheme = back_end_url.scheme or ("https" if ".verta.ai" in socket else "http")
    socket = back_end_url.netloc + back_end_url.path.rstrip('/')

    # set auth HTTP headers
    auth = {_utils._GRPC_PREFIX+'source': "PythonClient"}
    if email is None and dev_key is None:
        print("email and developer key not found; auth disabled")
    elif email is not None and dev_key is not None:
        auth.update({
            _utils._GRPC_PREFIX+'email': email,
            _utils._GRPC_PREFIX+'developer_key': dev_key,
            _utils._GRPC_PREFIX+'scheme': scheme,
        })
        # save credentials to env for other Verta Client features
        os.environ['VERTA_EMAIL'] = email
        os.environ['VERTA_DEV_KEY'] = dev_key
    else:
        raise ValueError("`email` and `dev_key` must be provided together")

    # TODO: verify connection
    # TODO: use `_connect` flag like `Client` for unit testing

    return _utils.Connection(scheme, socket, auth, max_retries, ignore_conn_err)
