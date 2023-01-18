# -*- coding: utf-8 -*-

from collections import defaultdict
import json
import threading

from typing import Any, Dict, Optional

_THREAD = threading.local()

def _init_thread_context() -> None:
    """Initialize our thread-local variable for storing logging context."""
    if not hasattr(_THREAD, "context"):
        _THREAD.context = dict()


def _get_thread_context() -> Dict[str, Any]:
    """
    Return the current thread-local context or initialize a new one.
    """
    _init_thread_context()
    return _THREAD.context


def _set_thread_context(context: Dict[str, Any]) -> Dict[str, Any]:
    """
    Sets the thread-local context, overwriting any existing values.
    """
    # init_thread_context()
    _THREAD.context = context
    return _THREAD.context


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
    local_context: Dict[str, Any] = _get_thread_context()
    local_context.update({key: value})
    _set_thread_context(local_context)


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
    def __init__(self, context: Optional[Dict[str, Any]] = None):
        self.context = context or dict()

    def __enter__(self):
        _set_thread_context(self.context)
        return self

    def __exit__(self, *args):
        """ Return thread-local context to prior state when exiting this context. """
        _set_thread_context(dict())

    def logs(self) -> Dict[str, Any]:
        """
        Return a JSON string representation of currently stored context.
        If JSON conversion fails, raise an error.
        """
        try:
            context: Dict[str, Any] = _get_thread_context()
            json.dumps(context)
            return context
        except json.JSONDecodeError as json_err:
            raise Exception("Unable to convert logging data to JSON") from json_err
        except TypeError as type_err:
            raise Exception("Unable to serialize log value to JSON.") from type_err
