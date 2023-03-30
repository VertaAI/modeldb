# -*- coding: utf-8 -*-
"""
Classes and functions to enable logging within a model's predict() function.

.. versionadded:: 0.22.0

"""

import json
import re
import threading
from typing import Any, Dict, Optional


_THREAD = threading.local()
_S3_REGEX = re.compile("[0-9a-zA-Z_-]+$")


def _get_thread_logs() -> Dict[str, Any]:
    """
    Return the 'logs' attribute of the thread-local variable.
    """
    if hasattr(_THREAD, "logs"):
        return _THREAD.logs
    return {}


def _set_thread_logs(logs: Dict[str, Any]) -> Dict[str, Any]:
    """
    Set the thread-local logs, overwriting values for existing keys.
    """
    _THREAD.logs = logs
    return _THREAD.logs


def _delete_thread_logs() -> None:
    """
    Drop the `log` attribute from the thread local variable.
    """
    delattr(_THREAD, "logs")


def _get_validate_flag() -> bool:
    """
    Return the 'validate' attribute of the thread local variable.
    """
    if hasattr(_THREAD, "validate"):
        return _THREAD.validate
    return False


def _set_validate_flag(flag: bool) -> bool:
    """
    Sets the thread-local validate flag boolean value.
    """
    _THREAD.validate = flag
    return _THREAD.validate


def _delete_validate_flag() -> None:
    """
    Drop the `validate` attribute from the thread local variable.
    """
    delattr(_THREAD, "validate")


def _validate_json(value: Any) -> str:
    """
    Attempt to serialize the provided value to JSON.
    Raise an error on failure.
    """
    try:
        return json.dumps(value)
    except TypeError:
        raise


def _validate_s3(value: str, pattern: re.Pattern = _S3_REGEX) -> None:
    """
    Check the provided value for any special characters that would
    violate Amazon S3 object key naming rules. Raise ValueError if any
    are found.
    """
    if len(value) > 100:
        raise ValueError(
            " provided key value must be 100 characters or less in length."
        )
    if not pattern.match(value):
        raise ValueError(
            f' provided value "{value}" contains non-alphanumeric '
            f"characters. (dashes and underscores permitted)"
        )


def log(key: str, value: Any) -> None:
    """
    Updates current logging dict with provided key and value.

    For use within the scope of a model's :meth:`~verta.registry.VertaModelBase.predict`
    method to collect logs.  All logs passed to this function are aggregated into a
    single dictionary within the context of one prediction, which is processed into
    storage after the prediction result is returned, to minimize added latency.

    .. note::
        Existing keys cannot be overwritten.  Multithreading of calls to log() within a
        model's predict() method is not currently supported.

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
        If `key` provided contains non-alphanumeric characters (Dashes `-`
        and underscores `_` are permitted), or the key already exists and the
        existing log cannot be overwritten.
    RuntimeError
        If this function is called outside the scope of any instance of
        :class:`~verta.runtime.context`.

    Examples
    --------
    .. code-block:: python
       :emphasize-lines: 15

        from verta import runtime

        # Sample model code:
        class MyModel(VertaModelBase):
            def __init__(self, artifacts):
                pass

            @verify_io
            def predict(self, x):
                embeddings = self.get_embeddings(x)
                return self.nn(embeddings)

            def get_embeddings(self, x):
                embedding = self.embedding[x]
                runtime.log("embedding", embedding)
                return embedding

    """
    if not hasattr(_THREAD, "logs"):
        raise RuntimeError(
            "no active verta.runtime.context() found; please ensure calls to"
            " verta.runtime.log() are made within your model's predict()"
            " method, or create a verta.runtime.context() for local testing"
        )
    if _get_validate_flag():
        _validate_json(value)
    _validate_s3(key)
    local_logs: Dict[str, Any] = _get_thread_logs()
    if key in local_logs.keys():
        raise ValueError(f' cannot overwrite existing value for "{key}"')
    local_logs.update({key: value})


class context:
    """
    Context manager for aggregating key-value pairs into a custom log entry.

    This class should not be instantiated directly in your model
    code. For all models deployed in Verta, the
    :meth:`~verta.registry.VertaModelBase.predict` method is wrapped inside a
    :class:`context` by default.

    For local testing, a single instance can be instantiated as in the
    provided example.

    Parameters
    ----------
    validate : bool, default False
        If ``True``, each individual call to :meth:`~verta.runtime.log`  will
        verify that the value provided is JSON serializable.

    Raises
    ------
    RuntimeError
        If this context manager instance is nested inside the context of an
        existing instance.

    Examples
    --------

    .. code-block:: python
       :emphasize-lines: 4,6,9

        import json
        from verta import runtime

        with runtime.context(validate=True) as ctx:  # validate values are JSON-serializable
            my_model.predict(x)  # your model, with your calls to runtime.log()
            print(json.dumps(ctx.logs()))

        # Same output after exiting the context manager:
        print(json.dumps(ctx.logs()))

    """

    def __init__(self, validate: Optional[bool] = False):
        self._validate = validate
        self._logs_dict = dict()

    def __enter__(self):
        """
        Ensure empty logs to start and set validation flag.
        """
        if hasattr(_THREAD, "logs"):  # If logs attribute exists already.
            raise RuntimeError(
                "nesting an instance of verta.runtime.context() inside"
                " an existing instance is not supported."
            )
        _set_thread_logs(dict())
        _set_validate_flag(self._validate)
        return self

    def __exit__(self, *args):
        """
        Capture the final collection of logs in an instance variable, and
        wipe the thread local variable clean.
        """
        self._logs_dict = _get_thread_logs()
        _delete_thread_logs()
        _delete_validate_flag()

    def logs(self) -> Dict[str, Any]:
        """
        Return the currently stored, thread-local logs from inside this
        context manager. If called after exiting the context manager, the
        final collection of logs captured at the time of exit is returned.

        Returns
        -------
        logs : Dict[str, Any]
            Dictionary of logs collected within this context manager.
        """
        return self._logs_dict or _get_thread_logs()
