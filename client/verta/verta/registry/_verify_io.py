# -*- coding: utf-8 -*-

import functools
import itertools
import json

from verta._vendored import six


_ALLOWED_INPUT_TYPES = (
    {
        dict,
        list,
        float,
        bool,
        type(None),
    }
    | set(six.string_types)
    | set(six.integer_types)
)
_ALLOWED_OUTPUT_TYPES = _ALLOWED_INPUT_TYPES | {tuple}

# for use like: `if getattr(model.predict, _DECORATED_FLAG, False)`
_DECORATED_FLAG = "_verta_verify_io"


def verify_io(f):
    """Decorator to typecheck I/O to ensure platform compatibility when deployed.

    Allowed input [#]_ and output [#]_ types are validated by Python's
    standard ``json`` library.

    Examples
    --------
    .. code-block:: python

        import numpy as np
        from verta.registry import verify_io, VertaModelBase

        class MyModel(VertaModelBase):
            def __init__(self, artifacts=None):
                pass

            @verify_io
            def predict(self, input):
                return [x**2 for x in input]

        model = MyModel()

        # succeeds; a list will be given to the deployed model as-is
        model.predict([1, 2, 3])

        # fails; deployed model won't be able to receieve a NumPy array
        model.predict(np.array([1, 2, 3]))

    References
    ----------
    .. [#] https://docs.python.org/3/library/json.html#json-to-py-table
    .. [#] https://docs.python.org/3/library/json.html#py-to-json-table

    Notes
    -----
    ``json.dumps()`` is used for its significantly faster performance compared
    to a manual recursive type-check, but there are a couple of edge cases
    where it will permit false negatives: most notably it will allow tuples
    which are actually passed as lists, though their interfaces are similar
    enough that misuse is unlikely.

    """

    @functools.wraps(f)
    def wrapper(self, *args, **kwargs):
        for arg in itertools.chain(args, kwargs.values()):
            _check_compatible_input(arg)

        output = f(self, *args, **kwargs)
        _check_compatible_output(output)
        return output

    setattr(wrapper, _DECORATED_FLAG, True)
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
    try:
        json.dumps(value)
    except TypeError as e:
        err_msg = "{}; {} must only contain types {}".format(
            str(e),
            value_name,
            sorted(map(lambda cls: cls.__name__, allowed_types)),
        )
        six.raise_from(TypeError(err_msg), None)
    except UnicodeDecodeError as e:
        # in Python 2, json.dumps() attempts to decode binary (unlike Python 3
        # which rejects it outright), so here we clarify the potential error
        err_msg = (
            "{}; {} cannot contain binary; consider encoding to base64 instead".format(
                str(e),
                value_name,
            )
        )
        six.raise_from(TypeError(err_msg), None)
