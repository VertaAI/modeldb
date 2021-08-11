# -*- coding: utf-8 -*-

from verta._protos.public.common import CommonService_pb2 as _CommonService

from . import _VertaComparison


class GreaterThanOrEqualTo(_VertaComparison):
    """
    A comparison: greater than or equal to the specified value.

    Parameters
    ----------
    value : float
        Value to be compared to.

    Examples
    --------
    .. code-block:: python

        from verta.monitoring.comparison import GreaterThanOrEqualTo
        assert GreaterThanOrEqualTo(.7).compare(.9)

    """

    _OPERATOR = _CommonService.OperatorEnum.GTE
    _SYMBOL = ">="

    def compare(self, other_value):
        return other_value >= self.value
