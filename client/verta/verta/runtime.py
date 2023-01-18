# -*- coding: utf-8 -*-

from collections import defaultdict
import json
import threading

from typing import Any, Dict

_THREAD = threading.local()

def init_thread_context() -> None:
    """Initialize our thread-local variable for storing logging context."""
    if not hasattr(_THREAD, "context"):
        _THREAD.context = defaultdict()


def get_thread_context() -> defaultdict:
    """
    Return the current thread-local context or initialize a new one.
    """
    init_thread_context()
    return _THREAD.context


def set_thread_context(context: defaultdict) -> defaultdict:
    """
    Sets the thread-local context, overwriting any existing values.
    """
    # init_thread_context()
    _THREAD.context = context
    return _THREAD.context


def log(**kwargs) -> None:
    """
    Log any number of key-value pairs to the current prediction context.
    Context is stored in thread-local variables.

    Parameters
    ----------
    kwargs : Any
        Arguments and values to be added as key-value pairs to the current
        logging context. Values must be serializable to JSON.

    Returns
    -------
    None
    """
    local_context: defaultdict = get_thread_context().copy()
    local_context.update(**kwargs)
    set_thread_context(local_context)


class context:
    """
    Context manager for aggregating key-value pairs into a custom log entry.
    Uses thread-local variables to store context in a thread-safe manner.

    Parameters
    ----------
    kwargs : Any
        Key-value pairs to be added to the current logging context.
    Attributes
    ----------
    as_json : str
        A JSON string representation of all the current saved logging context.
    """
    def __init__(self, **kwargs):
        self.new_context = kwargs
        self.prior_context = defaultdict()

    def __enter__(self):
        self.prior_context = get_thread_context()
        updated_context: defaultdict = self.prior_context.copy()
        updated_context.update(self.new_context)
        set_thread_context(updated_context)
        return self

    def __exit__(self, *args):
        """ Return thread-local context to prior state when exiting this context. """
        set_thread_context(self.prior_context)

    def logs(self) -> Dict[str, Any]:
        """
        Return a JSON string representation of currently stored context.
        If JSON conversion fails, raise an error.
        """
        try:
            context: defaultdict = get_thread_context()
            json.dumps(context)
            return dict(context)
        except json.JSONDecodeError as json_err:
            raise Exception("Unable to convert logging data to JSON") from json_err
        except TypeError as type_err:
            raise Exception("Unable to serialize log value to JSON.") from type_err
