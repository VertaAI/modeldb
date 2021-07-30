# -*- coding: utf-8 -*-

import functools

from verta.external import six


# TODO: check what the deal is with bytes and unicode
_ALLOWED_INPUT_TYPES = {
    dict,
    list,
    float,
    bool,
    type(None),
} | set(six.string_types) | set(six.integer_types)
_ALLOWED_OUTPUT_TYPES = _ALLOWED_INPUT_TYPES | {tuple}


def verify_io(f):
    """Decorator to typecheck I/O serializability within the Verta platform.

    Allowed input [1]_ and output [2]_ types are based on the documentation for
    Python's standard ``json`` library.

    Examples
    --------
    .. code-block:: python

        raise NotImplementedError

    References
    ----------
    .. [1] https://docs.python.org/3/library/json.html#json-to-py-table
    .. [2] https://docs.python.org/3/library/json.html#py-to-json-table

    """
    @functools.wraps(f)
    def wrapper(input):
        _check_compatible_input(input)
        output = f(input)
        _check_compatible_output(output)
        return output
    return wrapper


def _check_compatible_input(input):
    input_type = type(input)
    if input_type == dict:
        for key, val in six.iteritems(input):
            _check_compatible_input(key)
            _check_compatible_input(val)
    elif input_type == list:
        for el in input:
            _check_compatible_input(el)
    elif input_type not in _ALLOWED_INPUT_TYPES:
        raise TypeError("input must be one of types {}, not {}".format(_ALLOWED_INPUT_TYPES, input_type))


def _check_compatible_output(output):
    input_type = type(output)
    if input_type == dict:
        for key, val in six.iteritems(output):
            _check_compatible_output(key)
            _check_compatible_output(val)
    elif input_type == list:
        for el in output:
            _check_compatible_output(el)
    elif input_type not in _ALLOWED_OUTPUT_TYPES:
        raise TypeError("output must be one of types {}, not {}".format(_ALLOWED_OUTPUT_TYPES, input_type))
