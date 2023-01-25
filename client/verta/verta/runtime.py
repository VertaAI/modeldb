# -*- coding: utf-8 -*-
"""Classes and functions to enable logging within a model's predict() function.
.. versionadded:: 0.22.0

"""

import json
import re
import threading
from typing import Any, Dict, Optional


_THREAD = threading.local()
_S3_REGEX = re.compile("[0-9a-zA-Z_-]+$")


def _init_thread_logs() -> None:
    """
    Initialize our thread-local variable for storing logs.
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
    except TypeError:
        raise


def _validate_s3(
        value: str,
        pattern: re.Pattern = _S3_REGEX
    ) -> None:
    """
    Check the provided value for any special characters that would
    violate Amazon S3 object key naming rules. Raise ValueError if any
    are found.
    """
    if len(value) > 100:
        raise ValueError(" provided key value must be 100 characters or less in length.")
    if not pattern.match(value):
        raise ValueError(f" provided value \"{value}\" contains non-alphanumeric "
                         f"characters. (dashes and underscores permitted)")


def log(key: str, value: Any) -> None:
    """
    Updates current logging dict with provided key and value.
    For use within a model's :meth:`~verta.registry.VertaModelBase.predict`
    method to collect logs.

    .. note::
        Multithreading of calls to log() within a model's predict() method is not
        currently supported.

    Parameters
    ----------
    key : str
        String value to use as data label (key) in logs, with a limit of
        100 characters or less.
    value : Any
        Any `JSON serializable <https://docs.python.org/3/library/json.html#json.JSONEncoder>`__
        value you wish to include in the logs.
    Returns
    -------
    None

    Raises
    -------
    TypeError
        If `validate` was set to ``True`` on the active :class:`context`, and
        `value` is not a JSON-serializable type.
    ValueError
        If `key` provided contains non-alphanumeric characters.  Dashes `-`
        and underscores `_` are permitted.

    Examples
    --------
    .. code-block:: python
       :emphasize-lines: 11,14

        from verta import runtime

        # Sample model code:
        class MyModel(VertaModelBase):
            def __init__(self, artifacts):
                pass

            @verify.io
            def predict(self, x):
                embeddings = self.get_embeddings(x)
                return self.nn(embeddings)

            def get_embeddings(x):
                embedding = self.embedding[x]
                client.runtime.log("embedding", embedding)
                return embedding

    """
    if _get_validate_flag():
        _validate_json(value)
    _validate_s3(key)
    local_logs: Dict[str, Any] = _get_thread_logs()
    local_logs.update({key: value})


class context:
    """
    Context manager for aggregating key-value pairs into a custom log entry.
    It should not be necessary to instantiate this class directly unless you
    wish to export log entries manually to a resource other than Amazon S3.

    For all models deployed in Verta with active endpoints, the :meth:`~verta.registry.VertaModelBase.predict`
    function of the model is wrapped inside an object of this class by default.
    Logs collected inside the `predict()` function are exported to S3.

    Parameters
    ----------
    validate : bool, default False
        If true, each individual call to :meth:`~verta.runtime.log`  will
        verify that the value provided is JSON serializable.

    Raises
    ------
    RuntimeError
        If logs has been added via :meth:`~verta.runtime.log()` outside the
        scope of the model's :meth:`~verta.registry.VertaModelBase.predict`
        method, or outside the scope of this context manager.

    Examples
    --------

    .. code-block:: python
       :emphasize-lines: 4,8,11

        import json
        from verta import runtime

        with client.runtime.context() as ctx:
            client.runtime.log("key", {"value": ...})
            print(json.dumps(ctx.logs()))

        # After exiting the context manager:
        final_log_entry = json.dumps(ctx.logs())

        # Output (both) = {"labels": {"model_type": "scikit_learn"}, "output": "this_is_output"}

    """
    def __init__(self, validate: Optional[bool] = False):
        self._validate = validate
        self._logs_dict = dict()

    def __enter__(self):
        """
        Ensure empty logs to start and set validation flag.
        """
        if _get_thread_logs():  # If not an empty dict.
            raise RuntimeError(
                " cannot overwrite prior logs. Please ensure all calls to"
                " runtime.log() are made within the predict() method of your"
                " model, or inside the scope of an instance of verta.runtime.context()."
            )
        _set_thread_logs(dict())
        if self._validate:
            _set_validate_flag(True)
        return self

    def __exit__(self, *args):
        """
        Capture the final complete log entry in an instance variable, ensure
        empty logs, and reset the validation flag upon exit.
        """
        self._logs_dict = _get_thread_logs()
        _set_thread_logs(dict())
        _set_validate_flag(False)

    def logs(self) -> Dict[str, Any]:
        """
        Return the currently stored, thread-local logs from inside this
        context manager. If called after exiting the context manager, the
        final complete log entry is returned.

        Returns
        -------
        logs : Dict[str, Any]
            Dictionary of logs collected within this context manager.
        """
        return  self._logs_dict or _get_thread_logs()
