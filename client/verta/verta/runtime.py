# -*- coding: utf-8 -*-
"""Classes and functions to enable context logging within predictions made against
models on the Verta Platform.

.. versionadded:: 0.22.0

"""

import json
import threading
from typing import Any, Dict, Optional


_THREAD = threading.local()

def _init_thread_logs() -> None:
    """
    Initialize our thread-local variable for storing logging context.
    """
    if not hasattr(_THREAD, 'logs'):
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
    _THREAD.logs = logs
    return _THREAD.logs


def _init_validate_flag() -> None:
    """
    Initialize our thread-local variable for the validation flag.
    """
    if not hasattr(_THREAD, 'validate'):
        _THREAD.validate = False


def _get_validate_flag() -> bool:
    """
    Return the current thread-local variable for validate or initialize a
    new boolean.
    """
    _init_validate_flag()
    return _THREAD.validate


def _set_validate_flag(flag: bool) -> bool:
    """
    Sets the thread-local validate flag boolean value.
    """
    _THREAD.validate = flag
    return _THREAD.validate


def _validate_json(value: Any) -> str:
    """
    Attempt to serialize the provided value to JSON.
    Raise an error on failure.
    """
    try:
        return json.dumps(value)
    except json.JSONDecodeError as json_err:
        print("provided data cannot be serialized into JSON for logging.")
        raise json_err
    except TypeError as type_err:
        print("provided data type cannot be converted to JSON for logging.")
        raise type_err


def log(key: str, value: Any) -> None:
    """
    Updates current logging context dict with provided key and value.
    For use within a model's ``predict()`` method to collect logging
    context, which is stored in thread-local variables.

    Parameters
    ----------
    key : str
        String value to use as data label (key) in logging context.
    value : Any
        Any JSON serializable value you wish to include in the context logs.

    Returns
    -------
    None

    Examples
    --------
    .. code-block:: python
       :emphasize-lines: 10,13

        from verta import runtime

        # Sample model code:
        class MyModel(VertaModelBase):
            def __init__(self, artifacts):
                "ok"

            def predict(self, data):
                logs = {'some_stuff': 'I_care_about'}
                runtime.log('my_logging_key', logs)
                prediction_output = 'some_output'
                more_logs = ['a', 'list', 'of', 'things']
                runtime.log('things', more_logs)
                return prediction_output

    """
    if _get_validate_flag():
        _validate_json(value)
    local_context: Dict[str, Any] = _get_thread_logs()
    local_context.update({key: value})
    _set_thread_logs(local_context)


class context:
    """
    Context manager for aggregating key-value pairs into a custom log entry.
    Uses thread-local variables to store context in a thread-safe manner.

    Parameters
    ----------
    validate : Optional[bool]
        If true, each individual call to ``runtime.log('key', value)`` will
        verify that the value provided is JSON serializable.

    Attributes
    ----------
    logs : Dict[str, Any]
        Dictionary of the current logging context.  If called after exiting
        the context manager, the final complete log entry is returned.
    """
    def __init__(self, validate: Optional[bool] = False):
        self.validate = validate
        self.logs_dict = dict()

    def __enter__(self):
        """
        Ensure an empty logging context to start and set validation flag.
        """
        _set_thread_logs(dict())
        if self.validate:
            _set_validate_flag(True)
        return self

    def __exit__(self, *args):
        """
        Capture the final complete log entry in a class variable, ensure an
        empty logging context, and reset the validation flag upon exit.
        """
        self.logs_dict = _get_thread_logs()
        _set_thread_logs(dict())
        _set_validate_flag(False)

    def logs(self) -> Dict[str, Any]:
        """
        Return the currently stored, thread-local logs from inside this
        context manager. JSON serialization is attempted on the whole
        dictionary and an error is raised on failure.

        If called after exiting the context manager, the final complete
        log entry is returned.
        """
        logs: Dict[str, Any] = self.logs_dict or _get_thread_logs()
        _validate_json(logs)
        return logs
