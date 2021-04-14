# -*- coding: utf-8 -*-

import abc

from verta.external import six


@six.add_metaclass(abc.ABCMeta)
class _VertaComparison(object):
    _OPERATOR = None  # variant of OperatorEnum
    _SYMBOL = None  # e.g. "=="

    def __init__(self, value):
        self._value = value

    def __repr__(self):
        return '<comparison "{} {}">'.format(
            self._SYMBOL,
            self.value,
        )

    @property
    def value(self):
        return self._value

    def _operator_as_proto(self):
        return self._OPERATOR
