# -*- coding: utf-8 -*-

import json
import threading
from typing import Any, Dict, Optional


_THREAD = threading.local()

def _init_thread_logs() -> None:
    """Initialize our thread-local variable for storing logging context."""
    if not hasattr(_THREAD, "logs"):
        _THREAD.logs = dict()


def _get_thread_logs() -> Dict[str, Any]:
    """
    Return the current thread-local logs or initialize a new dict.
    """
    _init_thread_logs()
    return _THREAD.logs


def _set_thread_logs(logs: Dict[str, Any]) -> Dict[str, Any]:
    """
    Sets the thread-local logs, overwriting any existing values.
    """
    # init_thread_context()
    _THREAD.logs = logs
    return _THREAD.logs


def log(key: str, value: Any) -> None:
    """
    Update current context dict with provided key and value.
    Context is stored in thread-local variables.

    Parameters
    ----------
    key : str
        String value to use as data label (key) in logging context.
    value : Any
        Any JSON serializable value you wish to include in the context logs.

    Returns
    -------
    None
    """
    local_context: Dict[str, Any] = _get_thread_logs()
    local_context.update({key: value})
    _set_thread_logs(local_context)


class context:
    """
    Context manager for aggregating key-value pairs into a custom log entry.
    Uses thread-local variables to store context in a thread-safe manner.

    Parameters
    ----------
    context : Optional[Dict[str, Any]]
        Dict to be merged with the current logging context.
    Attributes
    ----------
    as_json : str
        A JSON string representation of all the current saved logging context.
    """
    def __init__(self):
        self.log_dict = dict()

    def __enter__(self):
        """ Ensure an empty logging context to start. """
        _set_thread_logs(dict())
        return self

    def __exit__(self, *args):
        """ Ensure an empty logging context after exiting context manager. """
        self.log_dict = _get_thread_logs()
        _set_thread_logs(dict())

    def logs(self) -> Dict[str, Any]:
        """
        Return the currently stored, thread-local logs from inside this
        context manager.  If JSON conversion fails, an error is raised.

        Will return None after exiting context manager.
        """
        try:
            logs: Dict[str, Any] = self.log_dict
            json.dumps(logs)
            return logs
        except json.JSONDecodeError as json_err:
            raise Exception("Unable to convert logging data to JSON") from json_err
        except TypeError as type_err:
            raise Exception("Unable to serialize log value to JSON.") from type_err
