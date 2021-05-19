# -*- coding: utf-8 -*-

from verta._protos.public.common import CommonService_pb2 as _CommonService

from . import _VertaComparison


class EqualTo(_VertaComparison):
    """
    A comparison: equal to the specified value.

    Parameters
    ----------
    value : float
        Value to be compared to.

    Examples
    --------
    .. code-block:: python

        from verta.monitoring.comparison import EqualTo
        assert EqualTo(.5).compare(.5)

    """

    _OPERATOR = _CommonService.OperatorEnum.EQ
    _SYMBOL = "=="

    def compare(self, other_value):
        if isinstance(self.value, float) or isinstance(other_value, float):
            return self.isclose(other_value, self.value)
        else:
            return other_value == self.value
