# -*- coding: utf-8 -*-

import json
from typing import Any, Dict


class LogHandler:
    """
    Container class for collecting and dumping log entries.
    """
    def __init__(self):
        self.attributes: Dict[str, Any] = dict()

    def __repr__(self):
        return str(self.attributes)

    def add(self, **kwargs):
        # Overwrites existing key-values
        self.attributes.update(**kwargs)

    def wipe_clean(self):
        self.attributes = dict()

# Use a single handler object that is wiped clean after each log entry
# to minimize memory usage
LOG_HANDLER = LogHandler()


def log(**kwargs) -> None:
    """
    Log any number of key-value pairs to the current prediction context.
    Values are deleted upon completion of the active model's predict() method.

    Parameters
    ----------
    kwargs : Any
        Arguments and values to be added as key-value pairs to the current log record.
        Values must be serializable to JSON.

    Returns
    -------
    None
    """
    LOG_HANDLER.add(**kwargs)


class ContextLogger():
    """
    Context manager for aggregating key-value pairs into a custom log entry.
    Any logged data is deleted upon exiting the context.

    Attributes
    ----------
    as_json : str
        A JSON string representation of all the current saved logging context.
    """
    def __init__(self):
        self.context = LOG_HANDLER
        if bool(self.context.attributes): # if not empty dict
            self.context.wipe_clean()

    def log(self, **kwargs) -> None:
        self.context.add(**kwargs)

    def __enter__(self):
        return self

    def __exit__(self, *args):
        # Handler attributes get deleted when context is exited.
        self.context.wipe_clean()

    @property
    def as_json(self) -> str:
        """
        Return a JSON string representation of currently stored context attributes.
        If JSON conversion fails, return string of current context.
        """
        try:
            return json.dumps(self.context.attributes)
        except json.JSONDecodeError as json_err:
            raise Exception("Unable to convert logging data to JSON") from json_err
        except TypeError as type_err:
            raise Exception("Unable to serialize log value to JSON.") from type_err

