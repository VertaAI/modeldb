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
    _check_compatible_value_helper(
        value=input,
        value_name="input",
        allowed_types=_ALLOWED_INPUT_TYPES,
    )


def _check_compatible_output(output):
    _check_compatible_value_helper(
        value=output,
        value_name="output",
        allowed_types=_ALLOWED_OUTPUT_TYPES,
    )


def _check_compatible_value_helper(value, value_name, allowed_types):
    input_type = type(value)
    if input_type == dict:
        for key, val in six.iteritems(value):
            _check_compatible_value_helper(key, value_name, allowed_types)
            _check_compatible_value_helper(val, value_name, allowed_types)
    elif input_type == list:
        for el in value:
            _check_compatible_value_helper(el, value_name, allowed_types)
    elif input_type not in allowed_types:
        raise TypeError(
            "{} must be one of types {}, but found {}".format(
                value_name,
                allowed_types,
                input_type,
            )
        )
