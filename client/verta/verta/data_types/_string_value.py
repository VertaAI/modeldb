# -*- coding: utf-8 -*-

from ..external import six

from .._internal_utils import arg_handler

from . import _VertaDataType


class StringValue(_VertaDataType):
    """
    Representation of a string.

    Parameters
    ----------
    value : str
        String.

    Examples
    --------
    .. code-block:: python

        from verta.data_types import StringValue
        data = StringValue("RNN")
        run.log_attribute("architecture", data)

    """

    _TYPE_NAME = "stringValue"
    _VERSION = "v1"

    @arg_handler.args_to_builtin(ignore_self=True)
    def __init__(self, value):
        if not isinstance(value, six.string_types):
            raise TypeError("`value` must be a string, not {}".format(type(value)))

        self._value = value

    def _as_dict(self):
        return self._as_dict_inner(
            {
                "value": self._value,
            }
        )

    @classmethod
    def _from_dict_inner(cls, d):
        data = d[cls._TYPE_NAME]
        return cls(value=data["value"])

    def diff(self, other):
        """Calculate the difference between `other` and this value.

        Parameters
        ----------
        other : :class:`StringValue`
            Value to calculate difference from.

        Returns
        -------
        int
            0 if the strings are equal, otherwise 1.

        """
        if not isinstance(other, type(self)):
            raise TypeError(
                "`other` must be type {}, not {}".format(type(self), type(other))
            )

        return 0 if self._value == other._value else 1
