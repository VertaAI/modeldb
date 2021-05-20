# -*- coding: utf-8 -*-

from verta._protos.public.common import CommonService_pb2 as _CommonService

from . import _VertaComparison


class LessThan(_VertaComparison):
    """
    A comparison: less than the specified value.

    Parameters
    ----------
    value : float
        Value to be compared to.

    Examples
    --------
    .. code-block:: python

        from verta.monitoring.comparison import LessThan
        assert LessThan(.3).compare(.1)

    """

    _OPERATOR = _CommonService.OperatorEnum.LT
    _SYMBOL = "<"

    def compare(self, other_value):
        return other_value < self.value
