# -*- coding: utf-8 -*-

import abc
import math

from verta.external import six


@six.add_metaclass(abc.ABCMeta)
class _VertaComparison(object):
    """
    Base class for comparisons. Not for external use.

    """

    _OPERATOR = None  # variant of OperatorEnum
    _SYMBOL = None  # e.g. "=="

    def __init__(self, value):
        self._value = value

    def __repr__(self):
        return "<comparison ({} {})>".format(
            self._SYMBOL,
            self.value,
        )

    @staticmethod
    def isclose(a, b, rel_tol=1e-9, abs_tol=0):
        if six.PY3:
            # pylint: disable=no-member
            return math.isclose(a, b, rel_tol=rel_tol, abs_tol=abs_tol)
        else:
            # equivalent implementation
            # https://docs.python.org/3/library/math.html#math.isclose
            return abs(a - b) <= max(rel_tol * max(abs(a), abs(b)), abs_tol)

    @property
    def value(self):
        return self._value

    def _operator_as_proto(self):
        return self._OPERATOR

    @classmethod
    def _from_proto(cls, msg, value):
        """
        Returns an comparison object.

        Parameters
        ----------
        msg : int
            Variant of ``OperatorEnum``.
        value : obj
            Value to be compared against.

        Returns
        -------
        :class:`_VertaComparison` subclass

        """
        for subcls in cls.__subclasses__():
            if msg == subcls._OPERATOR:
                return subcls(value)

        raise ValueError("operator {} not recognized".format(msg))

    @abc.abstractmethod
    def compare(self, other_value):
        """
        Compares `other_value` with this comparison's value.

        Parameters
        ----------
        other_value : obj
            Value to be compared with this comparison's value.

        Returns
        -------
        bool
            Result of the comparison.

        """
        raise NotImplementedError
