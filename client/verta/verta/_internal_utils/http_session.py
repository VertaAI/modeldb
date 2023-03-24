# -*- coding: utf-8 -*-
""" Utilities for working with request.Session objects, and handling of retries """

# TODO: Unit test for prediction_input_unpack method
# TODO: Unit test for prediction_io_cleanup method
from typing import Dict, Optional, Set

import requests
from requests import Session
from requests.adapters import HTTPAdapter
from urllib3.util import Retry

DEFAULT_MAX_RETRIES: int = 13
DEFAULT_BACKOFF_FACTOR: float = 0.3
DEFAULT_STATUS_FORCELIST: Set = {404, 429, 500, 503, 504}


def retry_config(
    max_retries: int = DEFAULT_MAX_RETRIES,
    status_forcelist: Set[int] = DEFAULT_STATUS_FORCELIST,
    backoff_factor: float = DEFAULT_BACKOFF_FACTOR,
) -> Retry:
    """Return a Retry object with the given parameters"""
    return Retry(
        total=None,  # Let `status` param control retries on failed status codes
        connect=0,  # Connection errors should raise an exception immediately
        status=max_retries,
        other=0,  # Guard against infinite retry loops
        status_forcelist=status_forcelist,
        backoff_factor=backoff_factor,
        allowed_methods=False,
    )


def set_retry_config(
    session: Session,
    max_retries: int,
    status_forcelist: Set[int],
    backoff_factor: float,
) -> Session:
    """Creates a new Retry object with the given params and mounts it to
    the provided session in place of the existing retry config. This
    allows the DeployedModel.predict method to change the behavior of
    the Session on the fly without dropping open connections."""
    current_retry = session.get_adapter("https://").max_retries
    if (
        max_retries != current_retry.status
        or status_forcelist != current_retry.status_forcelist
        or backoff_factor != current_retry.backoff_factor
    ):
        new_retry_config = retry_config(
            max_retries=max_retries or DEFAULT_MAX_RETRIES,
            status_forcelist=status_forcelist or DEFAULT_STATUS_FORCELIST,
            backoff_factor=backoff_factor or DEFAULT_BACKOFF_FACTOR,
        )
        adapter = HTTPAdapter(max_retries=new_retry_config)
        session.mount(prefix="https://", adapter=adapter)
        session.mount(prefix="http://", adapter=adapter)
    return session


def init_session(retry: Retry) -> Session:
    """Instantiate a Session object with a custom Retry configuration mounted
    via http adapter"""
    adapter = HTTPAdapter(max_retries=retry)
    http_session = requests.Session()
    http_session.mount(
        prefix="https://",
        adapter=adapter,
    )
    http_session.mount(
        prefix="http://",
        adapter=adapter,
    )
    return http_session
