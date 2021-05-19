# -*- coding: utf-8 -*-

from verta._protos.public.common import CommonService_pb2 as _CommonService

from . import _VertaComparison


class NotEqualTo(_VertaComparison):
    """
    A comparison: not equal to the specified value.

    Parameters
    ----------
    value : float
        Value to be compared to.

    Examples
    --------
    .. code-block:: python

        from verta.monitoring.comparison import NotEqualTo
        assert NotEqualTo(.5).compare(.7)

    """

    _OPERATOR = _CommonService.OperatorEnum.NE
    _SYMBOL = "!="

    def compare(self, other_value):
        if isinstance(self.value, float) or isinstance(other_value, float):
            return not self.isclose(other_value, self.value)
        else:
            return other_value != self.value
